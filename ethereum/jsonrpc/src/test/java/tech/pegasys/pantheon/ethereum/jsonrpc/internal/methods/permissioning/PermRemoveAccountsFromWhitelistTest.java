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
package tech.pegasys.pantheon.ethereum.jsonrpc.internal.methods.permissioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import tech.pegasys.pantheon.ethereum.jsonrpc.internal.JsonRpcRequest;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.exception.InvalidJsonRpcParameters;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.parameters.JsonRpcParameter;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcError;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcErrorResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcResponse;
import tech.pegasys.pantheon.ethereum.jsonrpc.internal.response.JsonRpcSuccessResponse;
import tech.pegasys.pantheon.ethereum.permissioning.AccountWhitelistController;
import tech.pegasys.pantheon.ethereum.permissioning.AccountWhitelistController.RemoveResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PermRemoveAccountsFromWhitelistTest {

  @Mock private AccountWhitelistController accountWhitelist;
  private PermRemoveAccountsFromWhitelist method;

  @Before
  public void before() {
    method = new PermRemoveAccountsFromWhitelist(accountWhitelist, new JsonRpcParameter());
  }

  @Test
  public void getNameShouldReturnExpectedName() {
    assertThat(method.getName()).isEqualTo("perm_removeAccountsFromWhitelist");
  }

  @Test
  public void whenAccountsAreRemovedFromWhitelistShouldReturnTrue() {
    List<String> accounts = Arrays.asList("0x0", "0x1");
    JsonRpcResponse expectedResponse = new JsonRpcSuccessResponse(null, true);
    when(accountWhitelist.removeAccounts(eq(accounts))).thenReturn(RemoveResult.SUCCESS);

    JsonRpcResponse actualResponse = method.response(request(accounts));

    assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse);
  }

  @Test
  public void whenAccountIsInvalidShouldReturnInvalidAccountErrorResponse() {
    JsonRpcResponse expectedResponse =
        new JsonRpcErrorResponse(null, JsonRpcError.ACCOUNT_WHITELIST_INVALID_ENTRY);
    when(accountWhitelist.removeAccounts(any())).thenReturn(RemoveResult.ERROR_INVALID_ENTRY);

    JsonRpcResponse actualResponse = method.response(request(new ArrayList<>()));

    assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse);
  }

  @Test
  public void whenAccountIsAbsentShouldReturnAbsentAccountErrorResponse() {
    JsonRpcResponse expectedResponse =
        new JsonRpcErrorResponse(null, JsonRpcError.ACCOUNT_WHITELIST_ABSENT_ENTRY);
    when(accountWhitelist.removeAccounts(any())).thenReturn(RemoveResult.ERROR_ABSENT_ENTRY);

    JsonRpcResponse actualResponse = method.response(request(new ArrayList<>()));

    assertThat(actualResponse).isEqualToComparingFieldByField(expectedResponse);
  }

  @Test
  public void whenEmptyParamOnRequestShouldThrowInvalidJsonRpcException() {
    JsonRpcRequest request =
        new JsonRpcRequest("2.0", "perm_removeAccountsFromWhitelist", new Object[] {});

    final Throwable thrown = catchThrowable(() -> method.response(request));
    assertThat(thrown)
        .hasNoCause()
        .isInstanceOf(InvalidJsonRpcParameters.class)
        .hasMessage("Missing required json rpc parameter at index 0");
  }

  private JsonRpcRequest request(final List<String> accounts) {
    return new JsonRpcRequest("2.0", "perm_removeAccountsFromWhitelist", new Object[] {accounts});
  }
}
