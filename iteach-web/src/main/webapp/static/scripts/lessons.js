var Lessons = function () {
	
	var timePattern = /[0-9]{2}:[0-9]{2}/;
	
	function _parseTime (s) {
		if (!timePattern.test(s)) {
			throw "Wrong time format: " + s;
		} else {
			var colon = s.indexOf(':');
			var sHours = s.substring(0, colon);
			var sMinutes = s.substring(colon + 1);
			var timestamp = Number(sHours) * 60 + Number(sMinutes);
			return timestamp;
		}
	}
	
	function _formatTime (value) {
		var hours = Math.floor(value / 60);
		var minutes = value % 60;
		return application.formatTimeHM(hours, minutes);
	}
	
	function init() {
		$.widget("ui.timespinner", $.ui.spinner, {
			options : {
				// minutes
				step : 1,
				// hours
				page : 60
			},
			_parse : function(value) {
				if (typeof value === "string") {
					// already a timestamp
					if (Number(value) == value) {
						return Number(value);
					} else {
						return _parseTime(value);
					}
				}
				return value;
			},

			_format : function(value) {
				return _formatTime(value);
			}
		});
	}
	
	function deleteLesson (id) {
		application.confirmAndCall(
			loc('lesson.delete.prompt'),
			function () {
				$.ajax({
					type: 'DELETE',
					url: 'ui/teacher/lesson/{0}'.format(id),
					contentType: 'application/json',
					dataType: 'json',
					success: function (data) {
						if (data.success) {
							// Going back to the planning
							location = "gui/home";
						} else {
							application.displayError(loc('lesson.delete.error'));
						}
					},
					error: function (jqXHR, textStatus, errorThrown) {
					  	application.displayAjaxError (loc('lesson.delete.error'), jqXHR, textStatus, errorThrown);
					}
				});
			}
		);
	}
	
	function readDate () {
		var raw = $( "#lessonDate" ).datepicker("getDate");
		return application.formatDate(raw);
	}
	
	function timeFieldInit (selector) {
		$(selector).timespinner();
	}
	
	/**
	 * Reverts to the current date in case of error
	 */
	function validateDate(selector) {
		var raw = $(selector).datepicker("getDate");
		$(selector).datepicker("setDate", raw);
		return true;
	}
	
	function validateTime (selector) {
		var value = $(selector).val();
		if (timePattern.test(value)) {
			return true;
		} else {
			application.displayError(loc('lesson.error.timeformat', value));
			return false;
		}
	}
	
	function validateTimeRange (from, to) {
		var a = _parseTime($(from).val());
		var b = _parseTime($(to).val());
		if (a < b) {
			return true; 
		} else {
			application.displayError(loc('lesson.error.timeorder'));
			return false;
		}
	}
	
	function validateLesson () {
		return validateDate("#lessonDate")
			&& validateTime("#lessonFrom")
			&& validateTime("#lessonTo")
			&& validateTimeRange("#lessonFrom", "#lessonTo");
	}
	
	function lessonDialogInit () {
		// Date field
		$( "#lessonDate" ).attr("placeholder", i18n.dateCalendarFormat);
		$( "#lessonDate" ).datepicker( "destroy" );
		$( "#lessonDate" ).datepicker({
			showOtherMonths: true,
		    selectOtherMonths: true,
		    dateFormat: i18n.dateCalendarFormat
		});
		$( "#lessonDate" ).datepicker ("setDate", new Date ($( "#lessonDate" ).val()));
		// Time fields
		timeFieldInit("#lessonFrom");
		timeFieldInit("#lessonTo");
	}
	
	function createLesson (date, startTime, endTime, cancelFn, successFn) {
		application.dialog({
			id: 'lesson-dialog',
			title: loc('lesson.new'),
			width: 550,
			data: {
				lessonDate: date,
				lessonFrom: startTime,
				lessonTo: endTime,
				lessonStudent: '',
				lessonLocation: ''
			},
			submit: {
				name: loc('general.create'),
				action: function () {
					return submitCreateLesson(successFn);
				}
			},
			open: lessonDialogInit,
			cancel: cancelFn
		});		
	}
	
	function submitCreateLesson (successFn) {
		if (validateLesson()) {
			$.ajax({
				type: 'POST',
				url: 'ui/teacher/lesson',
				contentType: 'application/json',
				data: JSON.stringify({
					date: readDate($('#lessonDate').val()),
					from: $('#lessonFrom').val(),
					to: $('#lessonTo').val(),
					student: $('#lessonStudent').val(),
					location: $('#lessonLocation').val()
				}),
				dataType: 'json',
				success: function (data) {
					if (data.success) {
						successFn();
						$('#lesson-dialog').dialog('close');
					} else {
						application.displayError(loc('lesson.new.error'));
					}
				},
				error: function (jqXHR, textStatus, errorThrown) {
				  	if (jqXHR.responseText && jqXHR.responseText != '') {
				  		$('#lesson-dialog-error').html(jqXHR.responseText.htmlWithLines());
				  		$('#lesson-dialog-error').show();
				  	} else {
				  		application.displayAjaxError (loc('lesson.new.error'), jqXHR, textStatus, errorThrown);
				  	}
				}
			});
		}
		return false;
	}
	
	function editLesson () {
		var id = $('#lesson-id').val();
		var student = $('#lesson-student').val();
		var date = $('#lesson-date').val();
		var startTime = $('#lesson-from').val();
		var endTime = $('#lesson-to').val();
		var location = $('#lesson-location').val();
		application.dialog({
			id: 'lesson-dialog',
			title: loc('lesson.edit'),
			width: 550,
			data: {
				lessonDate: date,
				lessonFrom: startTime,
				lessonTo: endTime,
				lessonStudent: student,
				lessonLocation: location
			},
			submit: {
				name: loc('general.update'),
				action: function () {
					return submitEditLesson (id);
				}
			},
			open: lessonDialogInit
		});		
	}
	
	function submitEditLesson (id) {
		if (validateLesson()) {
			$.ajax({
				type: 'PUT',
				url: 'ui/teacher/lesson/{0}'.format(id),
				contentType: 'application/json',
				data: JSON.stringify({
					date: readDate($('#lessonDate').val()),
					from: $('#lessonFrom').val(),
					to: $('#lessonTo').val(),
					student: $('#lessonStudent').val(),
					location: $('#lessonLocation').val()
				}),
				dataType: 'json',
				success: function (data) {
					if (data.success) {
						location.reload();
					} else {
						application.displayError(loc('lesson.edit.error'));
					}
				},
				error: function (jqXHR, textStatus, errorThrown) {
				  	if (jqXHR.responseText && jqXHR.responseText != '') {
				  		$('#lesson-dialog-error').html(jqXHR.responseText.htmlWithLines());
				  		$('#lesson-dialog-error').show();
				  	} else {
				  		application.displayAjaxError (loc('lesson.edit.error'), jqXHR, textStatus, errorThrown);
				  	}
				}
			});
		}
		return false;
	}

	return {
		lessonDialogInit: lessonDialogInit,
		deleteLesson: deleteLesson,
		editLesson: editLesson,
		createLesson: createLesson,
		init: init
	};

} ();

$(document).ready(Lessons.init);