package com.habitsoft.kiyaa.util;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.UnsafeNativeLong;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.SerializationException;
import com.google.gwt.user.client.rpc.impl.AbstractSerializationStreamReader;
import com.google.gwt.user.client.rpc.impl.Serializer;

public class DictionaryConstantsSerializationStreamReader extends AbstractSerializationStreamReader {
    private static native int getLength(JavaScriptObject array) /*-{
    return array.length;
  }-*/;

  @UnsafeNativeLong
  private static native long readLong0(double low, double high) /*-{
    return [low, high];
  }-*/;

  int index;

  JavaScriptObject results;

  JavaScriptObject stringTable;

  private Serializer serializer;

  public DictionaryConstantsSerializationStreamReader(Serializer serializer) {
    this.serializer = serializer;
  }

  public void prepareToRead(JavaScriptObject encoded) throws SerializationException {
    results = encoded;
    index = getLength(results);
    super.prepareToRead(null); // Not used in the superclass (for now)

    if (getVersion() != SERIALIZATION_STREAM_VERSION) {
      throw new IncompatibleRemoteServiceException("Expecting version "
          + SERIALIZATION_STREAM_VERSION + " from server, got " + getVersion()
          + ".");
    }

    stringTable = readJavaScriptObject();
  }

  public native boolean readBoolean() /*-{
    return !!this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

  public native byte readByte() /*-{
    return this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

  public native char readChar() /*-{
    return this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

  public native double readDouble() /*-{
    return this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

  public native float readFloat() /*-{
    return this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

  public native int readInt() /*-{
    return this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

  public long readLong() {
    if (GWT.isScript()) {
      return readLong0(readDouble(), readDouble());
    } else {
      return (long) readDouble() + (long) readDouble();
    }
  }

  public native short readShort() /*-{
    return this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

  public String readString() {
    return getString(readInt());
  }

  @Override
  protected Object deserialize(String typeSignature)
      throws SerializationException {
    int id = reserveDecodedObjectIndex();
    Object instance = serializer.instantiate(this, typeSignature);
    rememberDecodedObject(id, instance);
    serializer.deserialize(this, instance, typeSignature);
    return instance;
  }

  @Override
  protected native String getString(int index) /*-{
    // index is 1-based
    return index > 0 ? this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::stringTable[index - 1] : null;
  }-*/;

  private native JavaScriptObject readJavaScriptObject() /*-{
    return this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::results[--this.@com.habitsoft.kiyaa.util.DictionaryConstantsSerializationStreamReader::index];
  }-*/;

}
