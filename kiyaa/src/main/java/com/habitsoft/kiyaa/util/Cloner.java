package com.habitsoft.kiyaa.util;

import java.io.Serializable;

public interface Cloner<T extends Serializable> {

	public T clone(T src);
}
