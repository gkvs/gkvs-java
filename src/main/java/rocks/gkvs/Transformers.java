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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.annotation.Nullable;

import com.google.common.base.Function;
import com.google.common.collect.Iterators;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.protobuf.ByteString;

import io.grpc.stub.StreamObserver;
import rocks.gkvs.protos.BatchValueResult;
import rocks.gkvs.protos.ListEntry;
import rocks.gkvs.protos.ListResult;
import rocks.gkvs.protos.StatusResult;
import rocks.gkvs.protos.ValueResult;

/**
 * 
 * Transformers
 *
 * Utils
 *
 * @author Alex Shvid
 * @date Jun 18, 2018 
 *
 */

final class Transformers {

	private Transformers() {
	}
	
	protected static List<Entry> toEntryList(ListResult result) {
		
		List<Entry> list = new ArrayList<Entry>(result.getEntryCount());
		
		for (ListEntry protoEntry : result.getEntryList()) {
			
			String name = protoEntry.getName();
			rocks.gkvs.value.Value value = parseValue(protoEntry.getPayload());
			
			list.add(new Entry(name, value));
			
		}
		
		return list;
		
	}
	
	protected static ListenableFuture<List<Entry>> toEntryList(ListenableFuture<ListResult> result) {
		return Futures.transform(result, ListEntryFn.IMPL);
	}
	
	protected enum ListEntryFn implements Function<ListResult,List<Entry>> {
		
		IMPL;

		@Override
		public List<Entry> apply(ListResult input) {
			return toEntryList(input);
		}
		
	}
	
	protected static rocks.gkvs.protos.Value toProto(rocks.gkvs.value.Value value) {
		rocks.gkvs.protos.Value.Builder builder = rocks.gkvs.protos.Value.newBuilder();
		
		ByteString.Output out = ByteString.newOutput();
		try {
			value.writeTo(out);
		} catch (IOException e) {
			throw new GkvsException("i/o error", e);
		}
		builder.setRaw(out.toByteString());
		
		return builder.build();
	}
	
	protected static rocks.gkvs.value.Value parseValue(ByteString payload) {
		
		if (payload.isEmpty()) {
			return rocks.gkvs.value.Value.nil();
		}
		
		return rocks.gkvs.value.Parser.parseValue(payload.newInput());
		
	}
	
	protected static rocks.gkvs.value.Value fromProto(rocks.gkvs.protos.Value proto) {
		return parseValue(getValuePayload(proto));
	}
	
	protected static ByteString getValuePayload(rocks.gkvs.protos.Value proto) {
		return proto.getRaw();
	}
	
	protected static Record toRecord(@Nullable Key requestKey, ValueResult result) {
		if (ProtocolUtils.isError(result)) {
			return new RecordError(requestKey, result);
		}
		else if (result.hasMetadata()) {
			return new RecordFound(requestKey, result);
		}
		else {
			return new RecordNotFound(requestKey, result);
		}
	}
	
	protected static Status toStatus(@Nullable Key requestKey, StatusResult result) {
		if (ProtocolUtils.isError(result)) {
			return new StatusError(requestKey, result);
		}
		else {
			return new StatusSuccess(requestKey, result);
		}
	}
	
	protected static ListenableFuture<Record> toRecord(@Nullable Key requestKey, ListenableFuture<ValueResult> result) {
		return Futures.transform(result, new SimpleKeyRecordFn(requestKey));
	}
	
	protected static ListenableFuture<Iterable<Record>> toBatchRecords(ListenableFuture<BatchValueResult> result, KeyResolver keyResolver) {
		return Futures.transform(result, new BatchKeyRecordFn(keyResolver));
	}
	
	protected static ListenableFuture<Status> toStatus(@Nullable Key requestKey, ListenableFuture<StatusResult> result) {
		return Futures.transform(result, new SimpleKeyStatusFn(requestKey));
	}
	
	protected static Iterator<Record> toRecords(Iterator<ValueResult> iterator) {
		return Iterators.transform(iterator, SimpleRecordFn.INS);
	}
	
	protected static Iterator<Status> toStatuses(Iterator<StatusResult> iterator) {
		return Iterators.transform(iterator, SimpleStatusFn.INS);
	}
	
	protected static Iterator<Record> toRecords(Iterator<ValueResult> iterator, KeyResolver keyResolver) {
		return Iterators.transform(iterator, new RecordFn(keyResolver));
	}
	
	protected static Iterable<Record> toRecords(final Iterable<ValueResult> iter, final KeyResolver keyResolver) {
		
		return new Iterable<Record>() {

			@Override
			public Iterator<Record> iterator() {
				return toRecords(iter.iterator(), keyResolver);
			}
			
		};
	}
	
	protected interface KeyResolver {
		
		@Nullable Key find(long requestId);
		
	}
	
	protected enum NullKeyResolver implements KeyResolver {
		
		INS;
		
		public Key find(long requestId) {
			return null;
		}
		
	}
	
	protected static final class SimpleKeyRecordFn implements Function<ValueResult, Record> {

		private final @Nullable Key requestKey;
		
		protected SimpleKeyRecordFn(@Nullable Key requestKey) {
			this.requestKey = requestKey;
		}
		
		public Record apply(ValueResult result) {
			return toRecord(requestKey, result);
		}
		
	}
	
	protected static final class BatchKeyRecordFn implements Function<BatchValueResult, Iterable<Record>> {

		private final KeyResolver keyResolver;
		
		protected BatchKeyRecordFn(KeyResolver keyResolver) {
			this.keyResolver = keyResolver;
		}
		
		public Iterable<Record> apply(BatchValueResult result) {
			return toRecords(result.getResultList(), keyResolver);
		}
		
	}
	
	protected static final class SimpleKeyStatusFn implements Function<StatusResult, Status> {

		private final @Nullable Key requestKey;
		
		protected SimpleKeyStatusFn(@Nullable Key requestKey) {
			this.requestKey = requestKey;
		}
		
		public Status apply(StatusResult result) {
			return toStatus(requestKey, result);
		}
		
	}
	
	protected enum SimpleRecordFn implements Function<ValueResult, Record> {

		INS;
		
		public Record apply(ValueResult result) {
			return toRecord(null, result);
		}
		
	}
	
	protected enum SimpleStatusFn implements Function<StatusResult, Status> {

		INS;
		
		public Status apply(StatusResult result) {
			return toStatus(null, result);
		}
		
	}
	
	protected static final class RecordFn implements Function<ValueResult, Record> {

		private final KeyResolver keyResolver;
		
		public RecordFn(KeyResolver keyResolver) {
			this.keyResolver = keyResolver;
		}

		public Record apply(ValueResult result) {
			return toRecord(keyResolver.find(result.getHeader().getTag()), result);
		}
		
	}
	
	protected static final class StatusFn implements Function<StatusResult, Status> {

		private final KeyResolver keyResolver;
		
		public StatusFn(KeyResolver keyResolver) {
			this.keyResolver = keyResolver;
		}

		public Status apply(StatusResult result) {
			return toStatus(keyResolver.find(result.getHeader().getTag()), result);
		}
		
	}
	
	protected static StreamObserver<ValueResult> observeRecords(Observer<Record> recordObserver, KeyResolver keyResolver) {
		return new StreamRecordObserverAdapter(recordObserver, keyResolver);
	}
	
	protected static StreamObserver<BatchValueResult> observeBatchRecords(Observer<Iterable<Record>> recordObserver, KeyResolver keyResolver) {
		return new StreamBatchRecordObserverAdapter(recordObserver, keyResolver);
	}
	
	protected static StreamObserver<StatusResult> observeStatuses(Observer<Status> statusObserver, KeyResolver keyResolver) {
		return new StreamStatusObserverAdapter(statusObserver, keyResolver);
	}
	
	protected static final class StreamRecordObserverAdapter implements StreamObserver<ValueResult> {

		private final Observer<Record> recordObserver;
		private final KeyResolver keyResolver;
		
		public StreamRecordObserverAdapter(Observer<Record> recordObserver, KeyResolver keyResolver) {
			this.recordObserver = recordObserver;
			this.keyResolver = keyResolver;
		}
		
		@Override
		public void onNext(ValueResult value) {
			recordObserver.onNext(Transformers.toRecord(keyResolver.find(value.getHeader().getTag()), value));
		}

		@Override
		public void onError(Throwable t) {
			recordObserver.onError(t);
		}

		@Override
		public void onCompleted() {
			recordObserver.onCompleted();
		}
		
	}
	
	protected static final class StreamBatchRecordObserverAdapter implements StreamObserver<BatchValueResult> {

		private final Observer<Iterable<Record>> recordObserver;
		private final KeyResolver keyResolver;
		
		public StreamBatchRecordObserverAdapter(Observer<Iterable<Record>> recordObserver, KeyResolver keyResolver) {
			this.recordObserver = recordObserver;
			this.keyResolver = keyResolver;
		}
		
		@Override
		public void onNext(BatchValueResult value) {
			recordObserver.onNext(Transformers.toRecords(value.getResultList(), keyResolver));
		}

		@Override
		public void onError(Throwable t) {
			recordObserver.onError(t);
		}

		@Override
		public void onCompleted() {
			recordObserver.onCompleted();
		}
		
	}
	
	protected static final class StreamStatusObserverAdapter implements StreamObserver<StatusResult> {

		private final Observer<Status> statusObserver;
		private final KeyResolver keyResolver;
		
		public StreamStatusObserverAdapter(Observer<Status> statusObserver, KeyResolver keyResolver) {
			this.statusObserver = statusObserver;
			this.keyResolver = keyResolver;
		}
		
		@Override
		public void onNext(StatusResult value) {
			statusObserver.onNext(Transformers.toStatus(keyResolver.find(value.getHeader().getTag()), value));
		}

		@Override
		public void onError(Throwable t) {
			statusObserver.onError(t);
		}

		@Override
		public void onCompleted() {
			statusObserver.onCompleted();
		}
		
	}
	
}
