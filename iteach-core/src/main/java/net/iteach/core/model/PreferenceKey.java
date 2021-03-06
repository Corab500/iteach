package net.iteach.core.model;

import net.iteach.core.validation.ValidationException;
import net.sf.jstring.MultiLocalizable;
import org.apache.commons.lang3.StringUtils;

import net.sf.jstring.Localizable;
import net.sf.jstring.LocalizableMessage;

import java.util.Collections;

public enum PreferenceKey {
	
	PLANNING_MIN_TIME("8") {

		@Override
		public Localizable validate(String value) {
			return validateRange(PREFERENCE_KEY_MIN_TIME, value, 1, 11);
		}
		
	},
	
	PLANNING_MAX_TIME("20") {

		@Override
		public Localizable validate(String value) {
			return validateRange(PREFERENCE_KEY_MAX_TIME, value, 13, 23);
		}
		
	},

    PLANNING_WEEKEND("true") {

        @Override
        public Localizable validate(String value) {
            return validateBoolean(PREFERENCE_KEY_WEEKEND, value);
        }

    },

    REPORT_TOTAL_DISPLAYED("true") {

        @Override
        public Localizable validate(String value) {
            return validateBoolean(PREFERENCE_KEY_REPORT_TOTAL_DISPLAYED, value);
        }

    };
	
	private static final String PREFERENCE_KEY_MIN_TIME = "preference.key.min_time";
    private static final String PREFERENCE_KEY_MAX_TIME = "preference.key.max_time";
    private static final String PREFERENCE_KEY_WEEKEND = "preference.key.weekend";
    private static final String PREFERENCE_KEY_REPORT_TOTAL_DISPLAYED = "preference.key.reportTotalDisplayed";
    private static final String PREFERENCE_VALIDATION_BOOLEAN = "preference.validation.boolean";
    private static final String PREFERENCE_VALIDATION_RANGE = "preference.validation.range";
	private static final String PREFERENCE_VALIDATION_INTEGER = "preference.validation.integer";
	
	private final String defaultValue;
	
	private PreferenceKey(String defaultValue) {
		this.defaultValue = defaultValue;
	}
	
	public String getDefaultValue() {
		return defaultValue;
	}

	public abstract Localizable validate(String value);

    protected Localizable validateBoolean(String key, String value) {
        if ("true".equals(value) || "false".equals(value)) {
            return null;
        } else {
            return new LocalizableMessage(
                    PREFERENCE_VALIDATION_BOOLEAN,
                    new LocalizableMessage(key));
        }
    }

	protected Localizable validateRange(String key, String value, int min, int max) {
		if (StringUtils.isNumeric(value)) {
			int n = Integer.parseInt(value, 10);
			if (n >= min && n <= max) {
				return null;
			} else {
				return new LocalizableMessage(
						PREFERENCE_VALIDATION_RANGE,
						new LocalizableMessage(key),
						min, max);
			}
		} else {
			return new LocalizableMessage(
					PREFERENCE_VALIDATION_INTEGER,
					new LocalizableMessage(key));
		}
	}

    public String validateAndFormat(String value) {
        // Validation
        Localizable message = validate(value);
        if (message != null) {
            throw createValidationException(message);
        } else {
            return format(value);
        }
    }

    private ValidationException createValidationException(Localizable message) {
        return new ValidationException(new MultiLocalizable(Collections.singletonList(message)));
    }

    private String format(String value) {
        return StringUtils.trim(value);
    }
}
