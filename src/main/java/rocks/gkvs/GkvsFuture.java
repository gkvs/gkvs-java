/*
 *
 * Copyright 2018-present GKVS authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package rocks.gkvs;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

/**
 * 
 * GkvsFuture
 *
 * Extended future class for GKVS needs 
 *
 * @author Alex Shvid
 * @date Jun 18, 2018 
 *
 * @param <T>
 */

public class GkvsFuture<T> extends CompletableFuture<T> {

	private final ListenableFuture<T> delegate; 
	
	protected GkvsFuture(ListenableFuture<T> delegate) {
		this.delegate = delegate;
		addCallbacks(this::complete, this::completeExceptionally);
	}
    
	protected static <T> GkvsFuture<T> from(ListenableFuture<T> delegate) {
		return new GkvsFuture<T>(delegate);
	}
	
    public void addCallbacks(Consumer<T> successCallback, Consumer<Throwable> failureCallback) {
        Futures.addCallback(delegate, new FutureCallback<T>() {
            @Override
            public void onSuccess(T result) {
                successCallback.accept(result);
            }

            @Override
            public void onFailure(Throwable t) {
                failureCallback.accept(t);

            }
        }, MoreExecutors.directExecutor());
    }
    
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        }
		 boolean result = delegate.cancel(mayInterruptIfRunning);
	     super.cancel(mayInterruptIfRunning);
	     return result;
	}

	@Override
	public boolean isCancelled() {
		return delegate.isCancelled();
	}

	@Override
	public boolean isDone() {
		return delegate.isDone();
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		return delegate.get();
	}
	
	public T getUnchecked() {
		try {
			return delegate.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new GkvsException("future exception", e);
		}
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		return delegate.get(timeout, unit);
	}
	
	public T getUnchecked(long timeout, TimeUnit unit) {
		try {
			return delegate.get(timeout, unit);
		} catch (InterruptedException | ExecutionException | TimeoutException e) {
			throw new GkvsException("future exception", e);
		}
	}
	
	public void addListener(Runnable listener) {
		delegate.addListener(listener, MoreExecutors.directExecutor());	
	}
	
	public void addListener(Runnable listener, Executor executor) {
		delegate.addListener(listener, executor);	
	}
	
}
