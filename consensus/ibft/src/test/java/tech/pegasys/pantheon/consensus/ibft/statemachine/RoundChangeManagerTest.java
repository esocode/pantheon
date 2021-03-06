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
package tech.pegasys.pantheon.consensus.ibft.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.consensus.ibft.ConsensusRoundIdentifier;
import tech.pegasys.pantheon.consensus.ibft.IbftContext;
import tech.pegasys.pantheon.consensus.ibft.IbftHelpers;
import tech.pegasys.pantheon.consensus.ibft.TestHelpers;
import tech.pegasys.pantheon.consensus.ibft.payload.MessageFactory;
import tech.pegasys.pantheon.consensus.ibft.payload.PreparePayload;
import tech.pegasys.pantheon.consensus.ibft.payload.PreparedCertificate;
import tech.pegasys.pantheon.consensus.ibft.payload.ProposalPayload;
import tech.pegasys.pantheon.consensus.ibft.payload.RoundChangePayload;
import tech.pegasys.pantheon.consensus.ibft.payload.SignedData;
import tech.pegasys.pantheon.consensus.ibft.validation.MessageValidator;
import tech.pegasys.pantheon.consensus.ibft.validation.RoundChangeMessageValidator;
import tech.pegasys.pantheon.crypto.SECP256K1.KeyPair;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.Util;
import tech.pegasys.pantheon.ethereum.db.WorldStateArchive;
import tech.pegasys.pantheon.ethereum.mainnet.BlockHeaderValidator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;

public class RoundChangeManagerTest {

  private RoundChangeManager manager;

  private final KeyPair proposerKey = KeyPair.generate();
  private final KeyPair validator1Key = KeyPair.generate();
  private final KeyPair validator2Key = KeyPair.generate();
  private final KeyPair nonValidatorKey = KeyPair.generate();

  private final ConsensusRoundIdentifier ri1 = new ConsensusRoundIdentifier(2, 1);
  private final ConsensusRoundIdentifier ri2 = new ConsensusRoundIdentifier(2, 2);
  private final ConsensusRoundIdentifier ri3 = new ConsensusRoundIdentifier(2, 3);
  private final List<Address> validators = Lists.newArrayList();

  @Before
  public void setup() {

    validators.add(Util.publicKeyToAddress(proposerKey.getPublicKey()));
    validators.add(Util.publicKeyToAddress(validator1Key.getPublicKey()));
    validators.add(Util.publicKeyToAddress(validator2Key.getPublicKey()));

    final ProtocolContext<IbftContext> protocolContext =
        new ProtocolContext<>(
            mock(MutableBlockchain.class), mock(WorldStateArchive.class), mock(IbftContext.class));

    @SuppressWarnings("unchecked")
    BlockHeaderValidator<IbftContext> headerValidator =
        (BlockHeaderValidator<IbftContext>) mock(BlockHeaderValidator.class);
    when(headerValidator.validateHeader(any(), any(), any(), any())).thenReturn(true);
    BlockHeader parentHeader = mock(BlockHeader.class);

    RoundChangeMessageValidator.MessageValidatorForHeightFactory messageValidatorFactory =
        mock(RoundChangeMessageValidator.MessageValidatorForHeightFactory.class);

    when(messageValidatorFactory.createAt(ri1))
        .thenAnswer(
            invocation ->
                new MessageValidator(
                    validators,
                    Util.publicKeyToAddress(proposerKey.getPublicKey()),
                    ri1,
                    headerValidator,
                    protocolContext,
                    parentHeader));
    when(messageValidatorFactory.createAt(ri2))
        .thenAnswer(
            invocation ->
                new MessageValidator(
                    validators,
                    Util.publicKeyToAddress(validator1Key.getPublicKey()),
                    ri2,
                    headerValidator,
                    protocolContext,
                    parentHeader));
    when(messageValidatorFactory.createAt(ri3))
        .thenAnswer(
            invocation ->
                new MessageValidator(
                    validators,
                    Util.publicKeyToAddress(validator2Key.getPublicKey()),
                    ri3,
                    headerValidator,
                    protocolContext,
                    parentHeader));

    final RoundChangeMessageValidator roundChangeMessageValidator =
        new RoundChangeMessageValidator(
            messageValidatorFactory,
            validators,
            IbftHelpers.calculateRequiredValidatorQuorum(
                IbftHelpers.calculateRequiredValidatorQuorum(validators.size())),
            2);
    manager = new RoundChangeManager(2, roundChangeMessageValidator);
  }

  private SignedData<RoundChangePayload> makeRoundChangeMessage(
      final KeyPair key, final ConsensusRoundIdentifier round) {
    MessageFactory messageFactory = new MessageFactory(key);
    return messageFactory.createSignedRoundChangePayload(round, Optional.empty());
  }

  private SignedData<RoundChangePayload> makeRoundChangeMessageWithPreparedCert(
      final KeyPair key,
      final ConsensusRoundIdentifier round,
      final List<KeyPair> prepareProviders) {
    Preconditions.checkArgument(!prepareProviders.contains(key));

    final MessageFactory messageFactory = new MessageFactory(key);

    final ConsensusRoundIdentifier proposalRound = TestHelpers.createFrom(round, 0, -1);
    final Block block = TestHelpers.createProposalBlock(validators, proposalRound.getRoundNumber());
    // Proposal must come from an earlier round.
    final SignedData<ProposalPayload> proposal =
        messageFactory.createSignedProposalPayload(proposalRound, block);

    final List<SignedData<PreparePayload>> preparePayloads =
        prepareProviders
            .stream()
            .map(
                k -> {
                  final MessageFactory prepareFactory = new MessageFactory(k);
                  return prepareFactory.createSignedPreparePayload(proposalRound, block.getHash());
                })
            .collect(Collectors.toList());

    final PreparedCertificate cert = new PreparedCertificate(proposal, preparePayloads);

    return messageFactory.createSignedRoundChangePayload(round, Optional.of(cert));
  }

  @Test
  public void rejectsInvalidRoundChangeMessage() {
    SignedData<RoundChangePayload> roundChangeData = makeRoundChangeMessage(nonValidatorKey, ri1);
    assertThat(manager.appendRoundChangeMessage(roundChangeData)).isEmpty();
    assertThat(manager.roundChangeCache.get(ri1)).isNull();
  }

  @Test
  public void acceptsValidRoundChangeMessage() {
    SignedData<RoundChangePayload> roundChangeData = makeRoundChangeMessage(proposerKey, ri2);
    assertThat(manager.appendRoundChangeMessage(roundChangeData)).isEmpty();
    assertThat(manager.roundChangeCache.get(ri2).receivedMessages.size()).isEqualTo(1);
  }

  @Test
  public void doesntAcceptDuplicateValidRoundChangeMessage() {
    SignedData<RoundChangePayload> roundChangeData = makeRoundChangeMessage(proposerKey, ri2);
    assertThat(manager.appendRoundChangeMessage(roundChangeData)).isEmpty();
    assertThat(manager.appendRoundChangeMessage(roundChangeData)).isEmpty();
    assertThat(manager.roundChangeCache.get(ri2).receivedMessages.size()).isEqualTo(1);
  }

  @Test
  public void becomesReadyAtThreshold() {
    SignedData<RoundChangePayload> roundChangeDataProposer =
        makeRoundChangeMessage(proposerKey, ri2);
    SignedData<RoundChangePayload> roundChangeDataValidator1 =
        makeRoundChangeMessage(validator1Key, ri2);
    assertThat(manager.appendRoundChangeMessage(roundChangeDataProposer))
        .isEqualTo(Optional.empty());
    assertThat(manager.appendRoundChangeMessage(roundChangeDataValidator1).isPresent()).isTrue();
  }

  @Test
  public void doesntReachReadyWhenSuppliedWithDifferentRounds() {
    SignedData<RoundChangePayload> roundChangeDataProposer =
        makeRoundChangeMessage(proposerKey, ri2);
    SignedData<RoundChangePayload> roundChangeDataValidator1 =
        makeRoundChangeMessage(validator1Key, ri3);
    assertThat(manager.appendRoundChangeMessage(roundChangeDataProposer))
        .isEqualTo(Optional.empty());
    assertThat(manager.appendRoundChangeMessage(roundChangeDataValidator1))
        .isEqualTo(Optional.empty());
    assertThat(manager.roundChangeCache.get(ri2).receivedMessages.size()).isEqualTo(1);
    assertThat(manager.roundChangeCache.get(ri3).receivedMessages.size()).isEqualTo(1);
  }

  @Test
  public void discardsRoundPreviousToThatRequested() {
    SignedData<RoundChangePayload> roundChangeDataProposer =
        makeRoundChangeMessage(proposerKey, ri1);
    SignedData<RoundChangePayload> roundChangeDataValidator1 =
        makeRoundChangeMessage(validator1Key, ri2);
    SignedData<RoundChangePayload> roundChangeDataValidator2 =
        makeRoundChangeMessage(validator2Key, ri3);
    assertThat(manager.appendRoundChangeMessage(roundChangeDataProposer))
        .isEqualTo(Optional.empty());
    assertThat(manager.appendRoundChangeMessage(roundChangeDataValidator1))
        .isEqualTo(Optional.empty());
    assertThat(manager.appendRoundChangeMessage(roundChangeDataValidator2))
        .isEqualTo(Optional.empty());
    manager.discardRoundsPriorTo(ri2);
    assertThat(manager.roundChangeCache.get(ri1)).isNull();
    assertThat(manager.roundChangeCache.get(ri2).receivedMessages.size()).isEqualTo(1);
    assertThat(manager.roundChangeCache.get(ri3).receivedMessages.size()).isEqualTo(1);
  }

  @Test
  public void stopsAcceptingMessagesAfterReady() {
    SignedData<RoundChangePayload> roundChangeDataProposer =
        makeRoundChangeMessage(proposerKey, ri2);
    SignedData<RoundChangePayload> roundChangeDataValidator1 =
        makeRoundChangeMessage(validator1Key, ri2);
    SignedData<RoundChangePayload> roundChangeDataValidator2 =
        makeRoundChangeMessage(validator2Key, ri2);
    assertThat(manager.appendRoundChangeMessage(roundChangeDataProposer))
        .isEqualTo(Optional.empty());
    assertThat(manager.appendRoundChangeMessage(roundChangeDataValidator1).isPresent()).isTrue();
    assertThat(manager.roundChangeCache.get(ri2).receivedMessages.size()).isEqualTo(2);
    assertThat(manager.appendRoundChangeMessage(roundChangeDataValidator2))
        .isEqualTo(Optional.empty());
    assertThat(manager.roundChangeCache.get(ri2).receivedMessages.size()).isEqualTo(2);
  }

  @Test
  public void roundChangeMessagesWithPreparedCertificateMustHaveSufficientPrepareMessages() {
    // Specifically, prepareMessage count is ONE LESS than the calculated quorum size (as the
    // proposal acts as the extra msg).
    // There are 3 validators, therefore, should only need 2 prepare message to be acceptable.

    // These tests are run at ri2, such that validators can be found for past round at ri1.
    SignedData<RoundChangePayload> roundChangeData =
        makeRoundChangeMessageWithPreparedCert(proposerKey, ri2, Collections.emptyList());
    assertThat(manager.appendRoundChangeMessage(roundChangeData)).isEmpty();
    assertThat(manager.roundChangeCache.get(ri2)).isNull();

    roundChangeData =
        makeRoundChangeMessageWithPreparedCert(
            proposerKey, ri2, Lists.newArrayList(validator1Key, validator2Key));
    assertThat(manager.appendRoundChangeMessage(roundChangeData)).isEmpty();
    assertThat(manager.roundChangeCache.get(ri2).receivedMessages.size()).isEqualTo(1);
  }
}
