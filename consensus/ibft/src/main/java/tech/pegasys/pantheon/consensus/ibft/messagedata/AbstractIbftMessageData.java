/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package tech.pegasys.pantheon.consensus.ibft.messagedata;

import tech.pegasys.pantheon.consensus.ibft.payload.Payload;
import tech.pegasys.pantheon.consensus.ibft.payload.SignedData;
import tech.pegasys.pantheon.ethereum.p2p.api.MessageData;
import tech.pegasys.pantheon.ethereum.p2p.wire.AbstractMessageData;
import tech.pegasys.pantheon.util.bytes.BytesValue;

import java.util.function.Function;

public abstract class AbstractIbftMessageData extends AbstractMessageData {
  protected AbstractIbftMessageData(final BytesValue data) {
    super(data);
  }

  public abstract SignedData<? extends Payload> decode();

  protected static <T extends AbstractIbftMessageData> T fromMessageData(
      final MessageData messageData,
      final int messageCode,
      final Class<T> clazz,
      final Function<BytesValue, T> constructor) {
    if (clazz.isInstance(messageData)) {
      @SuppressWarnings("unchecked")
      T castMessage = (T) messageData;
      return castMessage;
    }
    final int code = messageData.getCode();
    if (code != messageCode) {
      throw new IllegalArgumentException(
          String.format(
              "MessageData has code %d and thus is not a %s", code, clazz.getSimpleName()));
    }

    return constructor.apply(messageData.getData());
  }
}
