/*
Copyright 2015 Prosoft, LLC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */

/**
 * <p>
 * A collection of classes that help with coding various patterns of handling objects that implement
 * {@link java.lang.AutoCloseable} (also called "resources") with the assistance of the {@code try}-with-resources
 * statement.
 * </p>
 * <p>
 * Java 7 introduced the {@code try}-with-resources statement and the companion {@code AutoCloseable} interface to
 * </p>
 * <p>
 * {@link java.time.LocalDate} stores a date without a time.
 * This stores a date like '2010-12-03' and could be used to store a birthday.
 * </p>
 * <p>
 * {@link java.time.LocalTime} stores a time without a date.
 * This stores a time like '11:30' and could be used to store an opening or closing time.
 * </p>
 * <p>
 * {@link java.time.LocalDateTime} stores a date and time.
 * This stores a date-time like '2010-12-03T11:30'.
 * </p>
 * <p>
 * {@link java.time.ZonedDateTime} stores a date and time with a time-zone.
 * This is useful if you want to perform accurate calculations of
 * dates and times taking into account the {@link java.time.ZoneId}, such as 'Europe/Paris'.
 * Where possible, it is recommended to use a simpler class without a time-zone.
 * The widespread use of time-zones tends to add considerable complexity to an application.
 * </p>
 * <h3>Creating Objects that Wrap Multiple Resources</h3>
 * <p>Sometimes, it makes sense to always use and release several resources together. One common way to achieve this
 * is by wrapping all those resources in a single object which also implements {@code AutoCloseable}.
 * </p>
 * <p>There are two critical aspects of properly writing any such wrapping object: a) initializing the various resources
 * to be wrapped without risking to leak any of them if the construction of the wrapper fails at any point; and b)
 * ensuring all of the wrapped resources are closed whenever the wrapper is closed. The
 * {@link com.prosoftnearshore.scope.CollectScope} and {@link com.prosoftnearshore.scope.WrapperScope} classes are
 * intended to assist in implementing the wrapper objects.
 * </p>
 * <h3>Additional value types</h3>
 * <p>
 * {@link java.time.Month} stores a month on its own.
 * This stores a single month-of-year in isolation, such as 'DECEMBER'.
 * </p>
 * <p>
 * {@link java.time.DayOfWeek} stores a day-of-week on its own.
 * This stores a single day-of-week in isolation, such as 'TUESDAY'.
 * </p>
 * <p>
 * {@link java.time.Year} stores a year on its own.
 * This stores a single year in isolation, such as '2010'.
 * </p>
 * <p>
 * {@link java.time.YearMonth} stores a year and month without a day or time.
 * This stores a year and month, such as '2010-12' and could be used for a credit card expiry.
 * </p>
 * <p>
 * {@link java.time.MonthDay} stores a month and day without a year or time.
 * This stores a month and day-of-month, such as '--12-03' and
 * could be used to store an annual event like a birthday without storing the year.
 * </p>
 * <p>
 * {@link java.time.OffsetTime} stores a time and offset from UTC without a date.
 * This stores a date like '11:30+01:00'.
 * The {@link java.time.ZoneOffset ZoneOffset} is of the form '+01:00'.
 * </p>
 * <p>
 * {@link java.time.OffsetDateTime} stores a date and time and offset from UTC.
 * This stores a date-time like '2010-12-03T11:30+01:00'.
 * This is sometimes found in XML messages and other forms of persistence,
 * but contains less information than a full time-zone.
 * </p>
 * <p>
 * <h3>Package specification</h3>
 * <p>
 * Unless otherwise noted, passing a null argument to a constructor or method in any class or interface
 * in this package will cause a {@link java.lang.NullPointerException NullPointerException} to be thrown.
 * The Javadoc "@param" definition is used to summarise the null-behavior.
 * The "@throws {@link java.lang.NullPointerException}" is not explicitly documented in each method.
 * </p>
 * <p>
 * All calculations should check for numeric overflow and throw either an {@link java.lang.ArithmeticException}
 * or a {@link java.time.DateTimeException}.
 * </p>
 * <p>
 * <h3>Design notes (non normative)</h3>
 * <p>
 * The API has been designed to reject null early and to be clear about this behavior.
 * A key exception is any method that takes an object and returns a boolean, for the purpose
 * of checking or validating, will generally return false for null.
 * </p>
 * <p>
 * The API is designed to be type-safe where reasonable in the main high-level API.
 * Thus, there are separate classes for the distinct concepts of date, time and date-time,
 * plus variants for offset and time-zone.
 * This can seem like a lot of classes, but most applications can begin with just five date/time types.
 * <ul>
 * <li>{@link java.time.Instant} - a timestamp</li>
 * <li>{@link java.time.LocalDate} - a date without a time, or any reference to an offset or time-zone</li>
 * <li>{@link java.time.LocalTime} - a time without a date, or any reference to an offset or time-zone</li>
 * <li>{@link java.time.LocalDateTime} - combines date and time, but still without any offset or time-zone</li>
 * <li>{@link java.time.ZonedDateTime} - a "full" date-time with time-zone and resolved offset from UTC/Greenwich</li>
 * </ul>
 * <p>
 * {@code Instant} is the closest equivalent class to {@code java.util.Date}.
 * {@code ZonedDateTime} is the closest equivalent class to {@code java.util.GregorianCalendar}.
 * </p>
 * <p>
 * Where possible, applications should use {@code LocalDate}, {@code LocalTime} and {@code LocalDateTime}
 * to better model the domain. For example, a birthday should be stored in a code {@code LocalDate}.
 * Bear in mind that any use of a {@linkplain java.time.ZoneId time-zone}, such as 'Europe/Paris', adds
 * considerable complexity to a calculation.
 * Many applications can be written only using {@code LocalDate}, {@code LocalTime} and {@code Instant},
 * with the time-zone added at the user interface (UI) layer.
 * </p>
 * <p>
 * The offset-based date-time types {@code OffsetTime} and {@code OffsetDateTime},
 * are intended primarily for use with network protocols and database access.
 * For example, most databases cannot automatically store a time-zone like 'Europe/Paris', but
 * they can store an offset like '+02:00'.
 * </p>
 * <p>
 * Classes are also provided for the most important sub-parts of a date, including {@code Month},
 * {@code DayOfWeek}, {@code Year}, {@code YearMonth} and {@code MonthDay}.
 * These can be used to model more complex date-time concepts.
 * For example, {@code YearMonth} is useful for representing a credit card expiry.
 * </p>
 * <p>
 * Note that while there are a large number of classes representing different aspects of dates,
 * there are relatively few dealing with different aspects of time.
 * Following type-safety to its logical conclusion would have resulted in classes for
 * hour-minute, hour-minute-second and hour-minute-second-nanosecond.
 * While logically pure, this was not a practical option as it would have almost tripled the
 * number of classes due to the combinations of date and time.
 * Thus, {@code LocalTime} is used for all precisions of time, with zeroes used to imply lower precision.
 * </p>
 * <p>
 * Following full type-safety to its ultimate conclusion might also argue for a separate class
 * for each field in date-time, such as a class for HourOfDay and another for DayOfMonth.
 * This approach was tried, but was excessively complicated in the Java language, lacking usability.
 * A similar problem occurs with periods.
 * There is a case for a separate class for each period unit, such as a type for Years and a type for Minutes.
 * However, this yields a lot of classes and a problem of type conversion.
 * Thus, the set of date-time types provided is a compromise between purity and practicality.
 * </p>
 * <p>
 * The API has a relatively large surface area in terms of number of methods.
 * This is made manageable through the use of consistent method prefixes.
 * <ul>
 * <li>{@code of} - static factory method</li>
 * <li>{@code parse} - static factory method focussed on parsing</li>
 * <li>{@code get} - gets the value of something</li>
 * <li>{@code is} - checks if something is true</li>
 * <li>{@code with} - the immutable equivalent of a setter</li>
 * <li>{@code plus} - adds an amount to an object</li>
 * <li>{@code minus} - subtracts an amount from an object</li>
 * <li>{@code to} - converts this object to another type</li>
 * <li>{@code at} - combines this object with another, such as {@code date.atTime(time)}</li>
 * </ul>
 * <p>
 * Multiple calendar systems is an awkward addition to the design challenges.
 * The first principal is that most users want the standard ISO calendar system.
 * As such, the main classes are ISO-only. The second principal is that most of those that want a
 * non-ISO calendar system want it for user interaction, thus it is a UI localization issue.
 * As such, date and time objects should be held as ISO objects in the data model and persistent
 * storage, only being converted to and from a local calendar for display.
 * The calendar system would be stored separately in the user preferences.
 * </p>
 * <p>
 * There are, however, some limited use cases where users believe they need to store and use
 * dates in arbitrary calendar systems throughout the application.
 * This is supported by {@link java.time.chrono.ChronoLocalDate}, however it is vital to read
 * all the associated warnings in the Javadoc of that interface before using it.
 * In summary, applications that require general interoperation between multiple calendar systems
 * typically need to be written in a very different way to those only using the ISO calendar,
 * thus most applications should just use ISO and avoid {@code ChronoLocalDate}.
 * </p>
 * <p>
 * The API is also designed for user extensibility, as there are many ways of calculating time.
 * The {@linkplain java.time.temporal.TemporalField field} and {@linkplain java.time.temporal.TemporalUnit unit}
 * API, accessed via {@link java.time.temporal.TemporalAccessor TemporalAccessor} and
 * {@link java.time.temporal.Temporal Temporal} provide considerable flexibility to applications.
 * In addition, the {@link java.time.temporal.TemporalQuery TemporalQuery} and
 * {@link java.time.temporal.TemporalAdjuster TemporalAdjuster} interfaces provide day-to-day
 * power, allowing code to read close to business requirements:
 * </p>
 * <pre>
 *   LocalDate customerBirthday = customer.loadBirthdayFromDatabase();
 *   LocalDate today = LocalDate.now();
 *   if (customerBirthday.equals(today)) {
 *     LocalDate specialOfferExpiryDate = today.plusWeeks(2).with(next(FRIDAY));
 *     customer.sendBirthdaySpecialOffer(specialOfferExpiryDate);
 *   }
 *
 * </pre>
 *
 * @since 0.1
 */
@javax.annotation.ParametersAreNonnullByDefault
package com.prosoftnearshore.scope;
