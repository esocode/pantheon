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
package tech.pegasys.pantheon.ethereum.mainnet;

import tech.pegasys.pantheon.config.GenesisConfigFile;
import tech.pegasys.pantheon.config.GenesisConfigOptions;
import tech.pegasys.pantheon.metrics.MetricsSystem;

import java.util.function.Function;

/** Provides {@link ProtocolSpec} lookups for mainnet hard forks. */
public class MainnetProtocolSchedule {

  public static final int DEFAULT_CHAIN_ID = 1;

  public static ProtocolSchedule<Void> create(final MetricsSystem metricsSystem) {
    return fromConfig(GenesisConfigFile.mainnet().getConfigOptions(), metricsSystem);
  }

  /**
   * Create a Mainnet protocol schedule from a config object
   *
   * @param config {@link GenesisConfigOptions} containing the config options for the milestone
   *     starting points
   * @param metricsSystem the {@link MetricsSystem} to use to record metrics
   * @return A configured mainnet protocol schedule
   */
  public static ProtocolSchedule<Void> fromConfig(
      final GenesisConfigOptions config, final MetricsSystem metricsSystem) {
    return new ProtocolScheduleFactory<>(
            metricsSystem, config, DEFAULT_CHAIN_ID, Function.identity())
        .createProtocolSchedule();
  }
}
