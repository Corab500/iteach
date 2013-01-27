package net.iteach.service.impl;

import static net.iteach.service.db.SQLUtils.dateToDB;
import static net.iteach.service.db.SQLUtils.timeToDB;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;
import javax.validation.Validator;

import net.iteach.api.CommentsService;
import net.iteach.api.CoordinatesService;
import net.iteach.api.LessonService;
import net.iteach.api.model.CommentEntity;
import net.iteach.api.model.CoordinateEntity;
import net.iteach.core.model.Ack;
import net.iteach.core.model.Comment;
import net.iteach.core.model.CommentFormat;
import net.iteach.core.model.Comments;
import net.iteach.core.model.CommentsForm;
import net.iteach.core.model.ID;
import net.iteach.core.model.Lesson;
import net.iteach.core.model.LessonChange;
import net.iteach.core.model.LessonDetails;
import net.iteach.core.model.LessonForm;
import net.iteach.core.model.LessonRange;
import net.iteach.core.model.Lessons;
import net.iteach.core.model.SchoolSummary;
import net.iteach.core.model.SchoolSummaryWithCoordinates;
import net.iteach.core.model.StudentLesson;
import net.iteach.core.model.StudentLessons;
import net.iteach.core.model.StudentSummary;
import net.iteach.core.model.StudentSummaryWithCoordinates;
import net.iteach.core.validation.LessonFormValidation;
import net.iteach.service.db.SQL;
import net.iteach.service.db.SQLUtils;
import net.sf.jstring.LocalizableMessage;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LessonServiceImpl extends AbstractServiceImpl implements LessonService {
	
	private final CoordinatesService coordinatesService;
	private final CommentsService commentsService;

	@Autowired
	public LessonServiceImpl(DataSource dataSource, Validator validator, CoordinatesService coordinatesService, CommentsService commentsService) {
		super(dataSource, validator);
		this.coordinatesService = coordinatesService;
		this.commentsService = commentsService;
	}
	
	@Override
	@Transactional(readOnly = true)
	public StudentLessons getLessonsForStudent(int userId, int id, LocalDate date) {
		// Check for the associated teacher
		checkTeacherForStudent(userId, id);
		// From: first day of the month
		String from = date.withDayOfMonth(1).toString();
		// To: last day of the month
		String to = date.withDayOfMonth(date.dayOfMonth().getMaximumValue()).toString();
		// All lessons
		List<StudentLesson> lessons = getNamedParameterJdbcTemplate().query(
			SQL.LESSONS_FOR_STUDENT,
			params("id", id).addValue("from", from).addValue("to", to),
			new RowMapper<StudentLesson> () {
				@Override
				public StudentLesson mapRow(ResultSet rs, int rowNum)
						throws SQLException {
					return new StudentLesson(
							rs.getInt("id"),
							SQLUtils.dateFromDB(rs.getString("pdate")),
							SQLUtils.timeFromDB(rs.getString("pfrom")),
							SQLUtils.timeFromDB(rs.getString("pto")),
							rs.getString("location")
							);
				}
			}
		);
		// Total hours
		BigDecimal hours = BigDecimal.ZERO;
		for (StudentLesson lesson : lessons) {
			hours = hours.add(getHours(lesson.getFrom(), lesson.getTo()));
		}
		// OK
		return new StudentLessons(
			date,
			lessons,
			hours
		);
	}

	@Override
	@Transactional(readOnly = true)
	public Lessons getLessonsForTeacher(int userId, LessonRange range) {
		// TODO Validation
		return new Lessons(
				getNamedParameterJdbcTemplate().query(
						SQL.LESSONS,
						params("teacher", userId)
							.addValue("from", range.getFrom().toString())
							.addValue("to", range.getTo().toString()),
						new RowMapper<Lesson>() {
							@Override
							public Lesson mapRow(ResultSet rs, int rowNum)
									throws SQLException {
								SchoolSummary school = new SchoolSummary(
										rs.getInt("SCHOOL_ID"),
										rs.getString("SCHOOL_NAME"),
										rs.getString("SCHOOL_COLOR"),
										SQLUtils.moneyFromDB(rs, "SCHOOL_HRATE"));
								StudentSummary student = new StudentSummary(
										rs.getInt("STUDENT_ID"),
										rs.getString("STUDENT_SUBJECT"),
										rs.getString("STUDENT_NAME"),
										school);
								return new Lesson(
										rs.getInt("id"),
										student,
										SQLUtils.dateFromDB(rs.getString("pdate")),
										SQLUtils.timeFromDB(rs.getString("pfrom")),
										SQLUtils.timeFromDB(rs.getString("pto")),
										rs.getString("location")
										);
							}
						}));
	}
	
	@Override
	@Transactional(readOnly = true)
	public LessonDetails getLessonDetails(int userId, int id) {
		checkTeacherForLesson(userId, id);
		return getNamedParameterJdbcTemplate().queryForObject(
			SQL.LESSON_DETAILS,
			params("id", id),
			new RowMapper<LessonDetails>() {
				@Override
				public LessonDetails mapRow(ResultSet rs, int rowNum) throws SQLException {
					int studentId = rs.getInt("STUDENT_ID");
					int schoolId = rs.getInt("SCHOOL_ID");
					SchoolSummaryWithCoordinates school = new SchoolSummaryWithCoordinates(
							schoolId,
							rs.getString("SCHOOL_NAME"),
							rs.getString("SCHOOL_COLOR"),
							SQLUtils.moneyFromDB(rs, "SCHOOL_HRATE"),
							coordinatesService.getCoordinates(CoordinateEntity.SCHOOL, schoolId));
					StudentSummaryWithCoordinates student = new StudentSummaryWithCoordinates(
							studentId,
							rs.getString("STUDENT_SUBJECT"),
							rs.getString("STUDENT_NAME"),
							school,
							coordinatesService.getCoordinates(CoordinateEntity.STUDENT, studentId));
					return new LessonDetails(
							rs.getInt("id"),
							student,
							SQLUtils.dateFromDB(rs.getString("pdate")),
							SQLUtils.timeFromDB(rs.getString("pfrom")),
							SQLUtils.timeFromDB(rs.getString("pto")),
							rs.getString("location")
							);
				}
			}
		);
	}
	
	@Override
	@Transactional
	public ID createLessonForTeacher(int userId, LessonForm form) {
        // Validation
        validate(form, LessonFormValidation.class);
        validate(form.getTo().isAfter(form.getFrom()), new LocalizableMessage("lesson.error.timeorder"));
        checkTeacherForStudent(userId, form.getStudent());
        
		GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
		int count = getNamedParameterJdbcTemplate().update(
				SQL.LESSON_CREATE,
				params("student", form.getStudent())
					.addValue("date", dateToDB(form.getDate()))
					.addValue("from", timeToDB(form.getFrom()))
					.addValue("to", timeToDB(form.getTo()))
					.addValue("location", form.getLocation()),
				keyHolder);
		return ID.count(count).withId(keyHolder.getKey().intValue());
	}
	
	@Override
	@Transactional
	public Ack editLessonForTeacher(int userId, int id, LessonForm form) {
        // Validation
        validate(form, LessonFormValidation.class);
		checkTeacherForLesson(userId, id);
		
		int count = getNamedParameterJdbcTemplate().update(
				SQL.LESSON_UPDATE,
				params("id", id)
					.addValue("student", form.getStudent())
					.addValue("date", dateToDB(form.getDate()))
					.addValue("from", timeToDB(form.getFrom()))
					.addValue("to", timeToDB(form.getTo()))
					.addValue("location", form.getLocation())
				);
		return Ack.one(count);
	}
	
	@Override
	@Transactional
	public Ack deleteLessonForTeacher(int teacherId, int id) {
		checkTeacherForLesson(teacherId, id);
		int count = getNamedParameterJdbcTemplate().update(SQL.LESSON_DELETE, params("id", id));
		return Ack.one(count);
	}
	
	@Override
	@Transactional
	public Ack changeLessonForTeacher(int userId, int lessonId, LessonChange change) {
		// Check for the associated teacher
		checkTeacherForLesson(userId, lessonId);
		// Loads the lesson range
		LessonRange range = getNamedParameterJdbcTemplate().queryForObject(
			SQL.LESSON_RANGE,
			params("id", lessonId),
			new RowMapper<LessonRange>() {
				@Override
				public LessonRange mapRow(ResultSet rs, int rowNum) throws SQLException {
					LocalDate date = SQLUtils.dateFromDB(rs.getString("pdate"));
					LocalTime from = SQLUtils.timeFromDB(rs.getString("pfrom"));
					LocalTime to = SQLUtils.timeFromDB(rs.getString("pto"));
					return new LessonRange(
						date.toLocalDateTime(from),
						date.toLocalDateTime(to));
				}
			});
		// Adjust the range
		LocalDateTime from = range.getFrom();
		LocalDateTime to = range.getTo();
		// Days?
		int dayDelta = change.getDayDelta();
		if (dayDelta != 0) {
			// Shifts both dates
			from = from.plusDays(dayDelta);
			to = to.plusDays(dayDelta);
		}
		// Minutes
		int minuteDelta = change.getMinuteDelta();
		if (minuteDelta != 0) {
			// Shifts only the end
			to = to.plusMinutes(minuteDelta);
		}
		// Redefines the lesson range
		LocalDate pdate = from.toLocalDate();
		LocalTime pfrom = from.toLocalTime();
		LocalTime pto = to.toLocalTime();
		// Updates the period
		int count = getNamedParameterJdbcTemplate().update(
				SQL.LESSON_RANGE_UPDATE,
				params("id", lessonId)
					.addValue("date", dateToDB(pdate))
					.addValue("from", timeToDB(pfrom))
					.addValue("to", timeToDB(pto))
				);
		return Ack.one(count);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Comments getLessonComments(int userId, int lessonId, int offset, int count, int maxlength, CommentFormat format) {
		// Check for the associated teacher
		checkTeacherForLesson(userId, lessonId);
		// Gets the comments
		return commentsService.getComments (CommentEntity.LESSON, lessonId, offset, count, maxlength, format);
	}
	
	@Override
	@Transactional(readOnly = true)
	public Comment getLessonComment(int userId, int lessonId, int commentId, CommentFormat format) {
		// Check for the associated teacher
		checkTeacherForLesson(userId, lessonId);
		// Gets the comment
		return commentsService.getComment (CommentEntity.LESSON, lessonId, commentId, format);
	}
	
	@Override
	@Transactional
	public Comment editLessonComment(int userId, int lessonId, CommentFormat format, CommentsForm form) {
		// Check for the associated teacher
		checkTeacherForLesson(userId, lessonId);
		// Creates the comment
		return commentsService.editComment (CommentEntity.LESSON, lessonId, format, form);
	}
	
	@Override
	@Transactional
	public Ack deleteLessonComment(int userId, int lessonId, int commentId) {
		// Check for the associated teacher
		checkTeacherForLesson(userId, lessonId);
		// Deletes the comment
		return commentsService.deleteComment (CommentEntity.LESSON, lessonId, commentId);
	}

}
