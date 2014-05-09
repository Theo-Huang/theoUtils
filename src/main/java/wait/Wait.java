package wait;

import com.google.common.base.Function;

public interface Wait <F>{
	<T> T until(Function<? super F, T> isTrue);
}
