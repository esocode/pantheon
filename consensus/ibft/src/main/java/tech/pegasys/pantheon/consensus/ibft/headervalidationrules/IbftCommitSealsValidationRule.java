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
package tech.pegasys.pantheon.consensus.ibft.headervalidationrules;

import static tech.pegasys.pantheon.consensus.ibft.IbftHelpers.calculateRequiredValidatorQuorum;

import tech.pegasys.pantheon.consensus.common.ValidatorProvider;
import tech.pegasys.pantheon.consensus.ibft.IbftBlockHashing;
import tech.pegasys.pantheon.consensus.ibft.IbftContext;
import tech.pegasys.pantheon.consensus.ibft.IbftExtraData;
import tech.pegasys.pantheon.ethereum.ProtocolContext;
import tech.pegasys.pantheon.ethereum.core.Address;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.mainnet.AttachedBlockHeaderValidationRule;

import java.util.Collection;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Ensures the commit seals in the block header were created by known validators (as determined by
 * tracking votes and validator state on the blockchain).
 *
 * <p>This also ensures sufficient commit seals exist in the block to make it valid.
 */
public class IbftCommitSealsValidationRule
    implements AttachedBlockHeaderValidationRule<IbftContext> {

  private static final Logger LOGGER = LogManager.getLogger();

  @Override
  public boolean validate(
      final BlockHeader header,
      final BlockHeader parent,
      final ProtocolContext<IbftContext> protocolContext) {
    final ValidatorProvider validatorProvider = protocolContext.getConsensusState().getVoteTally();
    final IbftExtraData ibftExtraData = IbftExtraData.decode(header.getExtraData());

    final List<Address> committers =
        IbftBlockHashing.recoverCommitterAddresses(header, ibftExtraData);

    return validateCommitters(committers, validatorProvider.getValidators());
  }

  private boolean validateCommitters(
      final Collection<Address> committers, final Collection<Address> storedValidators) {

    final int minimumSealsRequired = calculateRequiredValidatorQuorum(storedValidators.size());
    if (committers.size() < minimumSealsRequired) {
      LOGGER.trace(
          "Insufficient committers to seal block. (Required {}, received {})",
          minimumSealsRequired,
          committers.size());
      return false;
    }

    if (!storedValidators.containsAll(committers)) {
      LOGGER.trace("Not all committers are in the locally maintained validator list.");
      return false;
    }

    return true;
  }
}
