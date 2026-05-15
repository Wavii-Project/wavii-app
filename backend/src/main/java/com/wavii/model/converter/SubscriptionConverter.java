package com.wavii.model.converter;

import com.wavii.model.enums.Subscription;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = false)
public class SubscriptionConverter implements AttributeConverter<Subscription, String> {

    @Override
    public String convertToDatabaseColumn(Subscription attribute) {
        return attribute == null ? null : attribute.toDatabaseValue();
    }

    @Override
    public Subscription convertToEntityAttribute(String dbData) {
        return Subscription.fromDatabaseValue(dbData);
    }
}
