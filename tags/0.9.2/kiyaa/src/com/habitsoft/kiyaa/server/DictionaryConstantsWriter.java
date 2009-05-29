package com.habitsoft.kiyaa.server;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.zip.CRC32;

import org.apache.commons.lang.StringEscapeUtils;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamWriter;
import com.google.gwt.user.server.rpc.impl.SerializedInstanceReference;
import com.habitsoft.kiyaa.util.DictionaryConstants;

/**
 * A class you can use to serialize objects using the RPC algorithms, but
 * so they can be embedded into the HTML page in a <script> ... </script>
 * block and loaded using DictionaryConstants.
 * 
 * @author dobes
 */
public class DictionaryConstantsWriter extends AbstractSerializationStreamWriter {

    private static class SerializabilityUtil {

        public static final String DEFAULT_ENCODING = "UTF-8";

        /**
         * Comparator used to sort fields.
         */
        public static final Comparator<Field> FIELD_COMPARATOR = new Comparator<Field>() {
          public int compare(Field f1, Field f2) {
            return f1.getName().compareTo(f2.getName());
          }
        };

        /**
         * A permanent cache of all computed CRCs on classes. This is safe to do
         * because a Class is guaranteed not to change within the lifetime of a
         * ClassLoader (and thus, this Map). Access must be synchronized.
         */
        private static final Map<Class<?>, String> classCRC32Cache = new IdentityHashMap<Class<?>, String>();

        /**
         * A permanent cache of all serializable fields on classes. This is safe to do
         * because a Class is guaranteed not to change within the lifetime of a
         * ClassLoader (and thus, this Map). Access must be synchronized.
         */
        private static final Map<Class<?>, Field[]> classSerializableFieldsCache = new IdentityHashMap<Class<?>, Field[]>();

        /**
         * A permanent cache of all which classes onto custom field serializers. This
         * is safe to do because a Class is guaranteed not to change within the
         * lifetime of a ClassLoader (and thus, this Map). Access must be
         * synchronized.
         */
        private static final Map<Class<?>, Class<?>> classCustomSerializerCache = new IdentityHashMap<Class<?>, Class<?>>();

        private static final String JRE_SERIALIZER_PACKAGE = "com.google.gwt.user.client.rpc.core";

        private static final Map<String, String> SERIALIZED_PRIMITIVE_TYPE_NAMES = new HashMap<String, String>();

        private static final Set<Class<?>> TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES = new HashSet<Class<?>>();

        static {

          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(boolean.class.getName(), "Z");
          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(byte.class.getName(), "B");
          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(char.class.getName(), "C");
          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(double.class.getName(), "D");
          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(float.class.getName(), "F");
          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(int.class.getName(), "I");
          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(long.class.getName(), "J");
          SERIALIZED_PRIMITIVE_TYPE_NAMES.put(short.class.getName(), "S");

          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Boolean.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Byte.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Character.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Double.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Exception.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Float.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Integer.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Long.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Object.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Short.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(String.class);
          TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.add(Throwable.class);
        }

        public static Field[] applyFieldSerializationPolicy(Class<?> clazz) {
          Field[] serializableFields = getCachedSerializableFieldsForClass(clazz);
          if (serializableFields == null) {
            ArrayList<Field> fieldList = new ArrayList<Field>();
            Field[] fields = clazz.getDeclaredFields();
            for (Field field : fields) {
              if (fieldQualifiesForSerialization(field)) {
                fieldList.add(field);
              }
            }
            serializableFields = fieldList.toArray(new Field[fieldList.size()]);

            // sort the fields by name
            Arrays.sort(serializableFields, 0, serializableFields.length,
                FIELD_COMPARATOR);

            putCachedSerializableFieldsForClass(clazz, serializableFields);
          }

          return serializableFields;
        }

        public static SerializedInstanceReference decodeSerializedInstanceReference(
            String encodedSerializedInstanceReference) {
          final String[] components = encodedSerializedInstanceReference.split(SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR);
          return new SerializedInstanceReference() {
            public String getName() {
              return components.length > 0 ? components[0] : "";
            }

            public String getSignature() {
              return components.length > 1 ? components[1] : "";
            }
          };
        }

        public static String encodeSerializedInstanceReference(Class<?> instanceType) {
          return instanceType.getName()
              + SerializedInstanceReference.SERIALIZED_REFERENCE_SEPARATOR
              + getSerializationSignature(instanceType);
        }

        public static String getSerializationSignature(Class<?> instanceType) {
          String result = getCachedCRCForClass(instanceType);
          if (result == null) {
            CRC32 crc = new CRC32();
            try {
              generateSerializationSignature(instanceType, crc);
            } catch (UnsupportedEncodingException e) {
              throw new RuntimeException(
                  "Could not compute the serialization signature", e);
            }
            result = Long.toString(crc.getValue());
            putCachedCRCForClass(instanceType, result);
          }
          return result;
        }

        public static String getSerializedTypeName(Class<?> instanceType) {
          if (instanceType.isPrimitive()) {
            return SERIALIZED_PRIMITIVE_TYPE_NAMES.get(instanceType.getName());
          }

          return instanceType.getName();
        }

        /**
         * Returns the {@link Class} which can serialize the given instance type. Note
         * that arrays never have custom field serializers.
         */
        public static Class<?> hasCustomFieldSerializer(Class<?> instanceType) {
          assert (instanceType != null);
          if (instanceType.isArray()) {
            return null;
          }

          Class<?> result = getCachedSerializerForClass(instanceType);
          if (result != null) {
            // this class has a custom serializer
            return result;
          }
          if (containsCachedSerializerForClass(instanceType)) {
            // this class definitely has no custom serializer
            return null;
          }
          // compute whether this class has a custom serializer
          result = computeHasCustomFieldSerializer(instanceType);
          putCachedSerializerForClass(instanceType, result);
          return result;
        }

        /**
         * This method treats arrays in a special way.
         */
        private static Class<?> computeHasCustomFieldSerializer(Class<?> instanceType) {
          assert (instanceType != null);
          String qualifiedTypeName = instanceType.getName();
          ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
          String simpleSerializerName = qualifiedTypeName + "_CustomFieldSerializer";
          Class<?> customSerializer = getCustomFieldSerializer(classLoader,
              simpleSerializerName);
          if (customSerializer != null) {
            return customSerializer;
          }

          // Try with the regular name
          Class<?> customSerializerClass = getCustomFieldSerializer(classLoader,
              JRE_SERIALIZER_PACKAGE + "." + simpleSerializerName);
          if (customSerializerClass != null) {
            return customSerializerClass;
          }

          return null;
        }

        private static boolean containsCachedSerializerForClass(Class<?> instanceType) {
          synchronized (classCustomSerializerCache) {
            return classCustomSerializerCache.containsKey(instanceType);
          }
        }

        private static boolean excludeImplementationFromSerializationSignature(
            Class<?> instanceType) {
          if (TYPES_WHOSE_IMPLEMENTATION_IS_EXCLUDED_FROM_SIGNATURES.contains(instanceType)) {
            return true;
          }
          return false;
        }

        private static boolean fieldQualifiesForSerialization(Field field) {
          if (Throwable.class == field.getDeclaringClass()) {
            /**
             * Only serialize Throwable's detailMessage field; all others are ignored.
             * 
             * NOTE: Changing the set of fields that we serialize for Throwable will
             * necessitate a change to our JRE emulation's version of Throwable.
             */
            if ("detailMessage".equals(field.getName())) {
              assert (isNotStaticTransientOrFinal(field));
              return true;
            } else {
              return false;
            }
          } else {
            return isNotStaticTransientOrFinal(field);
          }
        }

        private static void generateSerializationSignature(Class<?> instanceType,
            CRC32 crc) throws UnsupportedEncodingException {
          crc.update(getSerializedTypeName(instanceType).getBytes(DEFAULT_ENCODING));

          if (excludeImplementationFromSerializationSignature(instanceType)) {
            return;
          }

          Class<?> customSerializer = hasCustomFieldSerializer(instanceType);
          if (customSerializer != null) {
            generateSerializationSignature(customSerializer, crc);
          } else if (instanceType.isArray()) {
            generateSerializationSignature(instanceType.getComponentType(), crc);
          } else if (!instanceType.isPrimitive()) {
            Field[] fields = applyFieldSerializationPolicy(instanceType);
            for (Field field : fields) {
              assert (field != null);

              crc.update(field.getName().getBytes(DEFAULT_ENCODING));
              crc.update(getSerializedTypeName(field.getType()).getBytes(
                  DEFAULT_ENCODING));
            }

            Class<?> superClass = instanceType.getSuperclass();
            if (superClass != null) {
              generateSerializationSignature(superClass, crc);
            }
          }
        }

        private static String getCachedCRCForClass(Class<?> instanceType) {
          synchronized (classCRC32Cache) {
            return classCRC32Cache.get(instanceType);
          }
        }

        private static Field[] getCachedSerializableFieldsForClass(Class<?> clazz) {
          synchronized (classSerializableFieldsCache) {
            return classSerializableFieldsCache.get(clazz);
          }
        }

        private static Class<?> getCachedSerializerForClass(Class<?> instanceType) {
          synchronized (classCustomSerializerCache) {
            return classCustomSerializerCache.get(instanceType);
          }
        }

        private static Class<?> getCustomFieldSerializer(ClassLoader classLoader,
            String qualifiedSerialzierName) {
          try {
            Class<?> customSerializerClass = Class.forName(qualifiedSerialzierName,
                false, classLoader);
            return customSerializerClass;
          } catch (ClassNotFoundException e) {
            return null;
          }
        }

        private static boolean isNotStaticTransientOrFinal(Field field) {
          /*
           * Only serialize fields that are not static, transient and final.
           */
          int fieldModifiers = field.getModifiers();
          return !Modifier.isStatic(fieldModifiers)
              && !Modifier.isTransient(fieldModifiers)
              && !Modifier.isFinal(fieldModifiers);
        }

        private static void putCachedCRCForClass(Class<?> instanceType, String crc32) {
          synchronized (classCRC32Cache) {
            classCRC32Cache.put(instanceType, crc32);
          }
        }

        private static void putCachedSerializableFieldsForClass(Class<?> clazz,
            Field[] serializableFields) {
          synchronized (classSerializableFieldsCache) {
            classSerializableFieldsCache.put(clazz, serializableFields);
          }
        }

        private static void putCachedSerializerForClass(Class<?> instanceType,
            Class<?> customFieldSerializer) {
          synchronized (classCustomSerializerCache) {
            classCustomSerializerCache.put(instanceType, customFieldSerializer);
          }
        }
      }

    /**
     * Builds a string that evaluates into an array containing the given elements. This class exists
     * to work around a bug in IE6/7 that limits the size of array literals.
     */
    public static class LengthConstrainedArray {
        public static final int MAXIMUM_ARRAY_LENGTH = 1 << 15;
        private static final String POSTLUDE = "])";
        private static final String PRELUDE = "].concat([";

        private final StringBuffer buffer;
        private int count = 0;
        private boolean needsComma = false;
        private int total = 0;

        public LengthConstrainedArray() {
            buffer = new StringBuffer();
        }

        public LengthConstrainedArray(int capacityGuess) {
            buffer = new StringBuffer(capacityGuess);
        }

        public void addToken(CharSequence token) {
            total++;
            if (count++ == MAXIMUM_ARRAY_LENGTH) {
                if (total == MAXIMUM_ARRAY_LENGTH + 1) {
                    buffer.append(PRELUDE);
                } else {
                    buffer.append("],[");
                }
                count = 0;
                needsComma = false;
            }

            if (needsComma) {
                buffer.append(",");
            } else {
                needsComma = true;
            }

            buffer.append(token);
        }

        public void addToken(int i) {
            addToken(String.valueOf(i));
        }

        @Override
        public String toString() {
            if (total > MAXIMUM_ARRAY_LENGTH) {
                return "[" + buffer.toString() + POSTLUDE;
            } else {
                return "[" + buffer.toString() + "]";
            }
        }
    }

    /**
     * Enumeration used to provided typed instance writers.
     */
    private enum ValueWriter {
        BOOLEAN {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeBoolean(((Boolean) instance).booleanValue());
            }
        },
        BYTE {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeByte(((Byte) instance).byteValue());
            }
        },
        CHAR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeChar(((Character) instance).charValue());
            }
        },
        DOUBLE {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeDouble(((Double) instance).doubleValue());
            }
        },
        FLOAT {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeFloat(((Float) instance).floatValue());
            }
        },
        INT {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeInt(((Integer) instance).intValue());
            }
        },
        LONG {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeLong(((Long) instance).longValue());
            }
        },
        OBJECT {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) throws SerializationException {
                stream.writeObject(instance);
            }
        },
        SHORT {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeShort(((Short) instance).shortValue());
            }
        },
        STRING {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                stream.writeString((String) instance);
            }
        };

        abstract void write(DictionaryConstantsWriter stream, Object instance) throws SerializationException;
    }

    /**
     * Enumeration used to provided typed vector writers.
     */
    private enum VectorWriter {
        BOOLEAN_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                boolean[] vector = (boolean[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeBoolean(vector[i]);
                }
            }
        },
        BYTE_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                byte[] vector = (byte[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeByte(vector[i]);
                }
            }
        },
        CHAR_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                char[] vector = (char[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeChar(vector[i]);
                }
            }
        },
        DOUBLE_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                double[] vector = (double[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeDouble(vector[i]);
                }
            }
        },
        FLOAT_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                float[] vector = (float[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeFloat(vector[i]);
                }
            }
        },
        INT_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                int[] vector = (int[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeInt(vector[i]);
                }
            }
        },
        LONG_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                long[] vector = (long[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeLong(vector[i]);
                }
            }
        },
        OBJECT_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) throws SerializationException {
                Object[] vector = (Object[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeObject(vector[i]);
                }
            }
        },
        SHORT_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                short[] vector = (short[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeShort(vector[i]);
                }
            }
        },
        STRING_VECTOR {
            @Override
            void write(DictionaryConstantsWriter stream, Object instance) {
                String[] vector = (String[]) instance;
                stream.writeInt(vector.length);
                for (int i = 0, n = vector.length; i < n; ++i) {
                    stream.writeString(vector[i]);
                }
            }
        };

        abstract void write(DictionaryConstantsWriter stream, Object instance) throws SerializationException;
    }

    /**
     * Map of {@link Class} objects to {@link ValueWriter}s.
     */
    private static final Map<Class<?>, ValueWriter> CLASS_TO_VALUE_WRITER = new IdentityHashMap<Class<?>, ValueWriter>();

    /**
     * Map of {@link Class} vector objects to {@link VectorWriter}s.
     */
    private static final Map<Class<?>, VectorWriter> CLASS_TO_VECTOR_WRITER = new IdentityHashMap<Class<?>, VectorWriter>();

    static {
        CLASS_TO_VECTOR_WRITER.put(boolean[].class, VectorWriter.BOOLEAN_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(byte[].class, VectorWriter.BYTE_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(char[].class, VectorWriter.CHAR_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(double[].class, VectorWriter.DOUBLE_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(float[].class, VectorWriter.FLOAT_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(int[].class, VectorWriter.INT_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(long[].class, VectorWriter.LONG_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(Object[].class, VectorWriter.OBJECT_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(short[].class, VectorWriter.SHORT_VECTOR);
        CLASS_TO_VECTOR_WRITER.put(String[].class, VectorWriter.STRING_VECTOR);

        CLASS_TO_VALUE_WRITER.put(boolean.class, ValueWriter.BOOLEAN);
        CLASS_TO_VALUE_WRITER.put(byte.class, ValueWriter.BYTE);
        CLASS_TO_VALUE_WRITER.put(char.class, ValueWriter.CHAR);
        CLASS_TO_VALUE_WRITER.put(double.class, ValueWriter.DOUBLE);
        CLASS_TO_VALUE_WRITER.put(float.class, ValueWriter.FLOAT);
        CLASS_TO_VALUE_WRITER.put(int.class, ValueWriter.INT);
        CLASS_TO_VALUE_WRITER.put(long.class, ValueWriter.LONG);
        CLASS_TO_VALUE_WRITER.put(Object.class, ValueWriter.OBJECT);
        CLASS_TO_VALUE_WRITER.put(short.class, ValueWriter.SHORT);
        CLASS_TO_VALUE_WRITER.put(String.class, ValueWriter.STRING);
    }

    /**
     * Returns the {@link Class} instance to use for serialization. Enumerations are serialized as
     * their declaring class while all others are serialized using their true class instance.
     */
    private static Class<?> getClassForSerialization(Object instance) {
        assert (instance != null);

        if (instance instanceof Enum) {
            Enum<?> e = (Enum<?>) instance;
            return e.getDeclaringClass();
        } else {
            return instance.getClass();
        }
    }
    private ArrayList<String> tokenList = new ArrayList<String>();

    private int tokenListCharCount;

    @Override
    public void prepareToWrite() {
        super.prepareToWrite();
        tokenList.clear();
        tokenListCharCount = 0;
    }

    public void serializeValue(Object value, Class<?> type) throws SerializationException {
        ValueWriter valueWriter = CLASS_TO_VALUE_WRITER.get(type);
        if (valueWriter != null) {
            valueWriter.write(this, value);
        } else {
            // Arrays of primitive or reference types need to go through writeObject.
            ValueWriter.OBJECT.write(this, value);
        }
    }

    /**
     * Build an array of JavaScript string literals that can be decoded by the client via the eval
     * function.
     * 
     * NOTE: We build the array in reverse so the client can simply use the pop function to remove
     * the next item from the list.
     */
    @Override
    public String toString() {
        // Build a JavaScript string (with escaping, of course).
        // We take a guess at how big to make to buffer to avoid numerous resizes.
        //
        int capacityGuess = 2 * tokenListCharCount + 2 * tokenList.size();
        LengthConstrainedArray stream = new LengthConstrainedArray(capacityGuess);
        writePayload(stream);
        writeStringTable(stream);
        writeHeader(stream);

        return stream.toString();
    }

    @Override
    public void writeLong(long fieldValue) {
        /*
         * Client code represents longs internally as an array of two Numbers. In order to make
         * serialization of longs faster, we'll send the component parts so that the value can be
         * directly reconstituted on the client.
         */
        double[] parts = makeLongComponents((int) (fieldValue >> 32), (int) fieldValue);
        assert parts.length == 2;
        writeDouble(parts[0]);
        writeDouble(parts[1]);
    }

    @Override
    protected void append(String token) {
        tokenList.add(token);
        if (token != null) {
            tokenListCharCount += token.length();
        }
    }

    @Override
    protected String getObjectTypeSignature(Object instance) {
        assert (instance != null);

        Class<?> clazz = getClassForSerialization(instance);
        return SerializabilityUtil.encodeSerializedInstanceReference(clazz);
    }

    @Override
    protected void serialize(Object instance, String typeSignature) throws SerializationException {
        assert (instance != null);

        Class<?> clazz = getClassForSerialization(instance);

        serializeImpl(instance, clazz);
    }

    /**
     * Serialize an instance that is an array. Will default to serializing the instance as an Object
     * vector if the instance is not a vector of primitives, Strings or Object.
     * 
     * @param instanceClass
     * @param instance
     * @throws SerializationException
     */
    private void serializeArray(Class<?> instanceClass, Object instance) throws SerializationException {
        assert (instanceClass.isArray());

        VectorWriter instanceWriter = CLASS_TO_VECTOR_WRITER.get(instanceClass);
        if (instanceWriter != null) {
            instanceWriter.write(this, instance);
        } else {
            VectorWriter.OBJECT_VECTOR.write(this, instance);
        }
    }

    private void serializeClass(Object instance, Class<?> instanceClass) throws SerializationException {
        assert (instance != null);

        Field[] serializableFields = SerializabilityUtil.applyFieldSerializationPolicy(instanceClass);
        for (Field declField : serializableFields) {
            assert (declField != null);

            boolean isAccessible = declField.isAccessible();
            boolean needsAccessOverride = !isAccessible && !Modifier.isPublic(declField.getModifiers());
            if (needsAccessOverride) {
                // Override the access restrictions
                declField.setAccessible(true);
            }

            Object value;
            try {
                value = declField.get(instance);
                serializeValue(value, declField.getType());

            } catch (IllegalArgumentException e) {
                throw new SerializationException(e);

            } catch (IllegalAccessException e) {
                throw new SerializationException(e);
            }
        }

        Class<?> superClass = instanceClass.getSuperclass();
        if (superClass != null && superClass != Object.class && (Serializable.class.isAssignableFrom(superClass) || IsSerializable.class.isAssignableFrom(superClass))) {
            serializeImpl(instance, superClass);
        }
    }

    private void serializeImpl(Object instance, Class<?> instanceClass) throws SerializationException {
        assert (instance != null);

        Class<?> customSerializer = SerializabilityUtil.hasCustomFieldSerializer(instanceClass);
        if (customSerializer != null) {
            // Use custom field serializer
            serializeWithCustomSerializer(customSerializer, instance, instanceClass);
        } else if (instanceClass.isArray()) {
            serializeArray(instanceClass, instance);
        } else if (instanceClass.isEnum()) {
            writeInt(((Enum<?>) instance).ordinal());
        } else {
            // Regular class instance
            serializeClass(instance, instanceClass);
        }
    }

    private void serializeWithCustomSerializer(Class<?> customSerializer, Object instance, Class<?> instanceClass) throws SerializationException {

        try {
            assert (!instanceClass.isArray());

            for (Method method : customSerializer.getMethods()) {
                if ("serialize".equals(method.getName())) {
                    method.invoke(null, this, instance);
                    return;
                }
            }
            throw new NoSuchMethodException("serialize");
        } catch (SecurityException e) {
            throw new SerializationException(e);

        } catch (NoSuchMethodException e) {
            throw new SerializationException(e);

        } catch (IllegalArgumentException e) {
            throw new SerializationException(e);

        } catch (IllegalAccessException e) {
            throw new SerializationException(e);

        } catch (InvocationTargetException e) {
            throw new SerializationException(e);
        }
    }

    /**
     * Notice that the field are written in reverse order that the client can just pop items out of
     * the stream.
     */
    private void writeHeader(LengthConstrainedArray stream) {
        stream.addToken(getFlags());
        stream.addToken(getVersion());
    }

    private void writePayload(LengthConstrainedArray stream) {
        ListIterator<String> tokenIterator = tokenList.listIterator(tokenList.size());
        while (tokenIterator.hasPrevious()) {
            stream.addToken(tokenIterator.previous());
        }
    }

    private void writeStringTable(LengthConstrainedArray stream) {
        LengthConstrainedArray tableStream = new LengthConstrainedArray();
        for (String s : getStringTable()) {
            tableStream.addToken(("'"+escapeJavaScriptString(s)+"'"));
        }
        stream.addToken(tableStream.toString());
    }

    final boolean inScriptTag;
    
    public DictionaryConstantsWriter(boolean inScriptTag) {
        this.inScriptTag = inScriptTag;
    }

    /**
     * Convert an object into a serialized string which could be used to deserialize it later inside
     * the code for DictionaryConstants.
     * 
     * @param object
     *            Object to serialize
     * @param clazz
     *            Target class expected by the deserializer (pass object.getClass() if you don't
     *            have anything better)
     * @param inScriptTag Whether any close tags should be escaped because this is in a <script> ... </script> block instead of its own js file.
     * @return A string representing the encoded form of the object; this is a javascript/json expression.
     * @throws SerializationException
     *             If there is a problem during serialization
     */
    public static String serializeObject(Object object, Class<?> clazz, boolean inScriptTag) throws SerializationException {
        DictionaryConstantsWriter stream = new DictionaryConstantsWriter(inScriptTag);
        stream.prepareToWrite();
        stream.serializeValue(object, clazz);
        return stream.toString();
    }

    /**
     * 
     * @param dictionaryName Javascript variable name
     * @param values Dictionary to serialize
     * @param clazz Target class expected by the deserializer (pass object.getClass() if you don't
     *            have anything better)
     * @param inScriptTag Whether any close tags should be escaped because this is in a <script> ... </script> block instead of its own js file.
     * @return A string representation of the given dictionary which can be deserialized by DictionaryConstants
     */
    public static String serializeMap(String dictionaryName, Map<String, Object> values, Class<? extends DictionaryConstants> clazz, boolean inScriptTag) throws SerializationException, SecurityException,
        NoSuchMethodException {
        StringBuffer sb = new StringBuffer();
        if (dictionaryName != null)
            sb.append(dictionaryName).append("=");
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            if (first)
                first = false;
            else
                sb.append(",\n");
            String key = entry.getKey();
            Object value = entry.getValue();
            Method method = clazz.getMethod(key);
            Class<?> returnType = method.getReturnType();
            appendValue(sb, key, value, returnType, inScriptTag);
        }
        sb.append("};");
        return sb.toString();
    }

    /**
     * Server-side serialization of the constants to a string.
     * 
     * @param <T>
     * @param dictionaryName Javascript variable name
     * @param values Dictionary to serialize
     * @param clazz Target class expected by the deserializer (pass object.getClass() if you don't
     *            have anything better)
     * @param inScriptTag Whether any close tags should be escaped because this is in a <script> ... </script> block instead of its own js file.
     * @return A string representation of the given dictionary which can be deserialized by DictionaryConstants
     */
    public static <T extends DictionaryConstants> String serializeConstants(String dictionaryName, T values, Class<T> clazz, boolean inScriptTag) throws SerializationException, SecurityException, NoSuchMethodException,
        IllegalArgumentException, IllegalAccessException, InvocationTargetException {
        StringBuffer sb = new StringBuffer();
        if (dictionaryName != null)
            sb.append(dictionaryName).append("=");
        sb.append('{');
        boolean first = true;
        for (Method method : clazz.getMethods()) {
            if (first)
                first = false;
            else
                sb.append(",\n");
            String key = method.getName();
            Object value = method.invoke(values);
            Class<?> returnType = method.getReturnType();
            appendValue(sb, key, value, returnType, inScriptTag);
        }
        sb.append('}');
        return sb.toString();
    }

    private static void appendValue(StringBuffer sb, String key, Object value, Class<?> returnType, boolean inScriptTag) throws SerializationException {
        sb.append('\'').append(escapeJavaScriptString(key)).append('\'').append(':');
        if (returnType.equals(boolean.class) || returnType.equals(int.class) || returnType.equals(double.class) || returnType.equals(float.class)) {
            sb.append(StringEscapeUtils.escapeJavaScript(String.valueOf(value)));
        } else if (returnType.equals(String.class)) {
            sb.append('\'').append(escapeJavaScriptString(String.valueOf(value))).append('\'');
        } else {
            sb.append(serializeObject(value, returnType, inScriptTag));
        }
    }

    /**
     * Escape a javascript string, assuming that it's being enclosed in single quotes. Any </script>
     * inside the string is broken up by sticking a '+' in the middle, effectively preventing naive
     * javascript tag parsers from seeing </script> and allowing that value to pass through
     * (theoretically).
     */
    private static Object escapeJavaScriptString(String s) {
        s = StringEscapeUtils.escapeJavaScript(s);
        s = s.replace("</", "<\\/");
        return s;
    }

}
