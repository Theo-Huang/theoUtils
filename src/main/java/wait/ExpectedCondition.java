package wait;

import com.google.common.base.Function;

public abstract interface ExpectedCondition<T> extends Function<Object, T> {}
