package org.terasology.entitySystem.pojo.persistence;

import com.google.common.base.Objects;
import com.google.common.collect.Maps;
import org.terasology.entitySystem.Component;
import org.terasology.protobuf.EntityData;
import sun.reflect.FieldInfo;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
* @author Immortius <immortius@gmail.com>
*/
public class ComponentMetadata<T extends Component> {
    private static final Logger logger = Logger.getLogger(ComponentMetadata.class.getName());

    private Map<String, FieldMetadata> fields = Maps.newHashMap();
    private Class<T> clazz;

    public ComponentMetadata(Class<T> componentClass) {
        this.clazz = componentClass;
    }

    public void addField(FieldMetadata fieldInfo) {
        fields.put(fieldInfo.getName().toLowerCase(Locale.ENGLISH), fieldInfo);
    }

    public T clone(T component) {
        try {
            T result = clazz.newInstance();
            for (FieldMetadata field : fields.values()) {
                field.setValue(result, field.getSerializationHandler().copy(field.getValue(component)));
            }
            return result;
        } catch (InstantiationException e) {
            logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
        }
        return null;
    }

    public EntityData.Component serialize(Component component) {
        EntityData.Component.Builder componentMessage = EntityData.Component.newBuilder();
        componentMessage.setType(component.getName());
        for (FieldMetadata field : fields.values()) {
            try {
                Object rawValue = null;
                rawValue = field.getValue(component);
                if (rawValue == null) continue;

                EntityData.Value value = field.getSerializationHandler().serialize(rawValue);
                if (value == null) continue;

                componentMessage.addField(EntityData.NameValue.newBuilder().setName(field.getName()).setValue(value).build());
            } catch (IllegalAccessException e) {
                logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
            } catch (InvocationTargetException e) {
                logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
            }
        }
        return componentMessage.build();
    }

    public EntityData.Component serialize(Component base, Component delta) {
        EntityData.Component.Builder componentMessage = EntityData.Component.newBuilder();
        componentMessage.setType(delta.getName());
        boolean changed = false;
        for (FieldMetadata field : fields.values()) {
            try {
                Object origValue = null;
                origValue = field.getValue(base);

                Object deltaValue = null;
                deltaValue = field.getValue(delta);

                if (Objects.equal(origValue, deltaValue)) continue;

                EntityData.Value value = field.getSerializationHandler().serialize(deltaValue);
                componentMessage.addField(EntityData.NameValue.newBuilder().setName(field.getName()).setValue(value).build());
                changed = true;
            } catch (IllegalAccessException e) {
                logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
            } catch (InvocationTargetException e) {
                logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
            }
        }
        if (changed) {
            return componentMessage.build();
        }
        return null;
    }

    public Component deserialize(EntityData.Component componentData) {
        try {
            Component component = clazz.newInstance();
            return deserializeOnto(component, componentData);
        } catch (InstantiationException e) {
            logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
        }
        return null;
    }

    public Component deserializeOnto(Component component, EntityData.Component componentData) {
        try {
            for (EntityData.NameValue field : componentData.getFieldList()) {
                FieldMetadata fieldInfo = fields.get(field.getName().toLowerCase(Locale.ENGLISH));
                if (fieldInfo == null)
                    continue;

                Object value = fieldInfo.getSerializationHandler().deserialize(field.getValue());
                if (value == null)
                    continue;
                fieldInfo.setValue(component, value);
            }
            return component;
        } catch (InvocationTargetException e) {
            logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
        } catch (IllegalAccessException e) {
            logger.log(Level.SEVERE, "Exception during serializing component type: " + clazz, e);
        }
        return null;
    }
}
