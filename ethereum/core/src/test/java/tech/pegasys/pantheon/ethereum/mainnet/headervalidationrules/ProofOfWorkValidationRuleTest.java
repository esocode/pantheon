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
package tech.pegasys.pantheon.ethereum.mainnet.headervalidationrules;

import static org.assertj.core.api.Assertions.assertThat;

import tech.pegasys.pantheon.ethereum.core.BlockHashFunction;
import tech.pegasys.pantheon.ethereum.core.BlockHeader;
import tech.pegasys.pantheon.ethereum.core.BlockHeaderBuilder;
import tech.pegasys.pantheon.ethereum.core.Hash;
import tech.pegasys.pantheon.ethereum.mainnet.MainnetProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ProtocolSchedule;
import tech.pegasys.pantheon.ethereum.mainnet.ScheduleBasedBlockHashFunction;
import tech.pegasys.pantheon.ethereum.mainnet.ValidationTestUtils;
import tech.pegasys.pantheon.metrics.noop.NoOpMetricsSystem;
import tech.pegasys.pantheon.util.uint.UInt256;

import java.io.IOException;
import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collection;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunWith(Parameterized.class)
public class ProofOfWorkValidationRuleTest {

  private final BlockHeader blockHeader;
  private final BlockHeader parentHeader;
  private final ProofOfWorkValidationRule validationRule;

  public ProofOfWorkValidationRuleTest(final long parentBlockNum, final long blockNum)
      throws IOException {
    blockHeader = ValidationTestUtils.readHeader(parentBlockNum);
    parentHeader = ValidationTestUtils.readHeader(blockNum);
    validationRule = new ProofOfWorkValidationRule();
  }

  @Parameters(name = "block {1}")
  public static Collection<Object[]> data() {

    return Arrays.asList(
        new Object[][] {
          {300005, 300006},
          {1200000, 1200001},
          {4400000, 4400001},
          {4400001, 4400002}
        });
  }

  @Test
  public void validatesValidBlocks() {
    assertThat(validationRule.validate(blockHeader, parentHeader)).isTrue();
  }

  @Test
  public void failsBlockWithZeroValuedDifficulty() {
    final BlockHeader header =
        BlockHeaderBuilder.fromHeader(blockHeader)
            .difficulty(UInt256.ZERO)
            .blockHashFunction(mainnetBlockHashFunction())
            .buildBlockHeader();
    assertThat(validationRule.validate(header, parentHeader)).isFalse();
  }

  @Test
  public void failsWithVeryLargeDifficulty() {
    final UInt256 largeDifficulty = UInt256.of(BigInteger.valueOf(2).pow(255));
    final BlockHeader header =
        BlockHeaderBuilder.fromHeader(blockHeader)
            .difficulty(largeDifficulty)
            .blockHashFunction(mainnetBlockHashFunction())
            .buildBlockHeader();
    assertThat(validationRule.validate(header, parentHeader)).isFalse();
  }

  @Test
  public void failsWithMisMatchedMixHash() {
    final Hash updateMixHash = Hash.wrap(blockHeader.getMixHash().asUInt256().minus(1L).getBytes());
    final BlockHeader header =
        BlockHeaderBuilder.fromHeader(blockHeader)
            .mixHash(updateMixHash)
            .blockHashFunction(mainnetBlockHashFunction())
            .buildBlockHeader();
    assertThat(validationRule.validate(header, parentHeader)).isFalse();
  }

  @Test
  public void failsWithMisMatchedNonce() {
    final long updatedNonce = blockHeader.getNonce() + 1;
    final BlockHeader header =
        BlockHeaderBuilder.fromHeader(blockHeader)
            .nonce(updatedNonce)
            .blockHashFunction(mainnetBlockHashFunction())
            .buildBlockHeader();
    assertThat(validationRule.validate(header, parentHeader)).isFalse();
  }

  private BlockHashFunction mainnetBlockHashFunction() {
    final ProtocolSchedule<Void> protocolSchedule =
        MainnetProtocolSchedule.create(new NoOpMetricsSystem());
    return ScheduleBasedBlockHashFunction.create(protocolSchedule);
  }
}
