package com.application.employee.service.deserializer;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class CustomLocalDateSerializer extends JsonSerializer<LocalDate> {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Override
    public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        String formattedDate = value.format(DATE_FORMATTER);
        gen.writeString(formattedDate);
    }
}

//import com.fasterxml.jackson.core.JsonParser;
//import com.fasterxml.jackson.databind.DeserializationContext;
//import com.fasterxml.jackson.databind.JsonDeserializer;
//
//import java.io.IOException;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//
//public class CustomLocalDateDeserializer extends JsonDeserializer<LocalDate> {
//    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
//    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
//
//    @Override
//    public LocalDate deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
//        String date = jsonParser.getText();
//        try {
//            if (date.contains("T")) {
//                return LocalDateTime.parse(date, DATETIME_FORMATTER).toLocalDate();
//            } else {
//                return LocalDate.parse(date, DATE_FORMATTER);
//            }
//        } catch (Exception e) {
//            throw new IOException("Failed to deserialize LocalDate", e);
//        }
//    }
//}
/*
In the original code, the CustomLocalDateDeserializer class implements the JsonDeserializer interface, which allows us to define custom deserialization logic for the LocalDate type.

The CustomLocalDateDeserializer class had a single deserialize method that handles the deserialization process. It receives a JsonParser and a DeserializationContext as parameters.

The original implementation attempted to parse the date string directly using the LocalDate.parse method. However, this caused an issue when the date string included a time component, as it expected a date-only format.

In the updated code, we made the following changes to handle both date-only and date-time formats:

We introduced two DateTimeFormatter instances: DATE_FORMATTER for date-only formats and DATETIME_FORMATTER for date-time formats. The date-only format is "yyyy-MM-dd", and the date-time format is "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'".

In the deserialize method, we retrieve the date string from the JsonParser and check if it contains the character "T" using the contains method. The presence of "T" indicates a date-time format.

If the date string contains "T", we parse it as a LocalDateTime using the DATETIME_FORMATTER and then convert it to a LocalDate using the toLocalDate method.

If the date string does not contain "T", we parse it directly as a LocalDate using the DATE_FORMATTER.

If any parsing errors occur, we throw an IOException with a custom error message.

By updating the CustomLocalDateDeserializer with these changes, it can now handle both date-only and date-time formats, providing more flexibility for parsing different date representations.

With the updated CustomLocalDateDeserializer, when deserializing a JSON payload that includes a date string, the deserialization process checks the format of the date string and uses the appropriate formatter to parse it correctly into a LocalDate object.

Overall, the changes made in the code allow for more robust date deserialization by supporting multiple date formats and handling them appropriately.
*/
