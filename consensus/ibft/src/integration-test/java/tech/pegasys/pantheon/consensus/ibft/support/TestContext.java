/*
 * Copyright 2019 ConsenSys AG.
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
package tech.pegasys.pantheon.consensus.ibft.support;

import tech.pegasys.pantheon.consensus.ibft.ConsensusRoundIdentifier;
import tech.pegasys.pantheon.consensus.ibft.payload.MessageFactory;
import tech.pegasys.pantheon.consensus.ibft.statemachine.IbftController;
import tech.pegasys.pantheon.consensus.ibft.statemachine.IbftFinalState;
import tech.pegasys.pantheon.ethereum.chain.MutableBlockchain;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.Block;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/*
Responsible for creating an environment in which integration testing can be conducted.

The test setup is an 'n' node network, one of which is the local node (i.e. the Unit Under Test).

There is some complexity with determining the which node is the proposer etc. THus necessitating
NetworkLayout and RoundSpecificNodeRoles concepts.
 */
public class TestContext {

  private final Map<Address, ValidatorPeer> remotePeers;
  private final MutableBlockchain blockchain;
  private final IbftController controller;
  private final IbftFinalState finalState;

  public TestContext(
      final Map<Address, ValidatorPeer> remotePeers,
      final MutableBlockchain blockchain,
      final IbftController controller,
      final IbftFinalState finalState) {
    this.remotePeers = remotePeers;
    this.blockchain = blockchain;
    this.controller = controller;
    this.finalState = finalState;
  }

  public Collection<ValidatorPeer> getRemotePeers() {
    return remotePeers.values();
  }

  public MutableBlockchain getBlockchain() {
    return blockchain;
  }

  public IbftController getController() {
    return controller;
  }

  public MessageFactory getLocalNodeMessageFactory() {
    return finalState.getMessageFactory();
  }

  public Block createBlockForProposal(final int round, final long timestamp) {
    return finalState
        .getBlockCreatorFactory()
        .create(blockchain.getChainHeadHeader(), round)
        .createBlock(timestamp);
  }

  public RoundSpecificNodeRoles getRoundSpecificRoles(final ConsensusRoundIdentifier roundId) {
    // This will return NULL if the LOCAL node is the proposer for the specified round
    final Address proposerAddress = finalState.getProposerForRound(roundId);
    final ValidatorPeer proposer = remotePeers.getOrDefault(proposerAddress, null);

    final List<ValidatorPeer> nonProposers = new ArrayList<>(remotePeers.values());
    nonProposers.remove(proposer);

    return new RoundSpecificNodeRoles(proposer, remotePeers.values(), nonProposers);
  }

  public NodeParams getLocalNodeParams() {
    return new NodeParams(finalState.getLocalAddress(), finalState.getNodeKeys());
  }

  public long getCurrentChainHeight() {
    return blockchain.getChainHeadBlockNumber();
  }
}
