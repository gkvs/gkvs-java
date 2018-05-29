/*
 *
 * Copyright 2018 gKVS authors.
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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import rocks.gkvs.Transformers.KeyResolver;
import rocks.gkvs.protos.BatchKeyOperation;
import rocks.gkvs.protos.BatchValueResult;
import rocks.gkvs.protos.KeyOperation;
import rocks.gkvs.protos.OutputOptions;
import rocks.gkvs.protos.RequestOptions;
import rocks.gkvs.protos.Select;

public final class MultiGet {

	private final GKVSClient instance;
	
	private final Map<Long, Key> keys = new HashMap<>();
	
	private Select.Builder selectOrNull;
	private boolean metadataOnly = false;
	private int timeoutMls = 0;
	private long pit = 0l;

	final KeyResolver keyResolver = new KeyResolver() {

		@Override
		public Key find(long requestId) {
			return keys.get(requestId);
		}
		
	};
	
	public MultiGet(GKVSClient instance) {
		this.instance = instance;
	}
	
	public MultiGet withTimeout(int timeoutMls) {
		this.timeoutMls = timeoutMls;
		return this;
	}
	
	public MultiGet withPit(long pit) {
		this.pit = pit;
		return this;
	}
	
	public MultiGet setKeys(Key...keys) {
		for (Key key : keys) {
			this.keys.put(instance.nextRequestId(), key);
		}
		return this;
	}
	
	public MultiGet setKeys(Iterator<Key> keys) {
		while(keys.hasNext()) {
			this.keys.put(instance.nextRequestId(), keys.next());
		}
		return this;
	}
	
	public MultiGet setKeys(Iterable<Key> keys) {
		setKeys(keys.iterator());
		return this;
	}
	
	public MultiGet addKey(Key key) {
		this.keys.put(instance.nextRequestId(), key);
		return this;
	}
	
	public MultiGet select(String column) {
		
		if (column == null) {
			throw new IllegalArgumentException("column is null");
		}		
		
		if (selectOrNull == null) {
			selectOrNull = Select.newBuilder();
		}
		selectOrNull.addColumn(column);
		return this;
	}
	
	private KeyOperation.Builder buildRequest(long requestId, Key key) {
		
		KeyOperation.Builder builder = KeyOperation.newBuilder();
		
		RequestOptions.Builder options = RequestOptions.newBuilder();
		options.setTimeout(timeoutMls);
		options.setPit(pit);
		options.setRequestId(requestId);
		
		builder.setOptions(options);
		builder.setKey(key.toProto());
		
		if (metadataOnly) {
			builder.setOutput(OutputOptions.METADATA_ONLY);
		}
		else {
			builder.setOutput(OutputOptions.VALUE_RAW);
		}
		
		if (selectOrNull != null) {
			builder.setSelect(selectOrNull);
		}
		
		return builder;
		
	}
	
	private BatchKeyOperation buildRequest() {
		
		BatchKeyOperation.Builder builder = BatchKeyOperation.newBuilder();
		
		for (Map.Entry<Long, Key> entry : keys.entrySet()) {
			builder.addOperation(buildRequest(entry.getKey(), entry.getValue()));
		}
		
		return builder.build();
		
	}
	
	public Iterable<Record> sync() {
		
		final BatchValueResult result = instance.getBlockingStub().multiGet(buildRequest());
				
		return Transformers.toRecords(result.getResultList(), keyResolver);
		
	}
	
	public GKVSFuture<Iterable<Record>> async() {
		
		ListenableFuture<BatchValueResult> result = instance.getFutureStub().multiGet(buildRequest());
		
		ListenableFuture<Iterable<Record>> transformedResult = Futures.transform(result, new Function<BatchValueResult, Iterable<Record>>() {

			@Override
			public Iterable<Record> apply(BatchValueResult input) {
				return Transformers.toRecords(input.getResultList(), keyResolver);
			}
			
		});
		
		return new GKVSFuture<Iterable<Record>>(transformedResult);
	}
	
}
