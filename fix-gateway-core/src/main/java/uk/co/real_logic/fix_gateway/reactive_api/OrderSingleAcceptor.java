/*
 * Copyright 2015 Real Logic Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.co.real_logic.fix_gateway.reactive_api;

import uk.co.real_logic.fix_gateway.MessageAcceptor;
import uk.co.real_logic.fix_gateway.fields.AsciiFieldFlyweight;
import uk.co.real_logic.fix_gateway.fields.DecimalFloatFlyweight;
import uk.co.real_logic.fix_gateway.fields.OrdTypeFlyweight;
import uk.co.real_logic.fix_gateway.fields.SideFlyweight;

/**
 *
 */
public interface OrderSingleAcceptor extends MessageAcceptor
{
    // Each field has a callback

    void onClOrdIDField(AsciiFieldFlyweight clOrdID);

    void onHandlInstField(char handlInst);

    void onSideField(SideFlyweight side);

    void onPriceField(DecimalFloatFlyweight price);

    void onOrdTypeField(OrdTypeFlyweight ordType);

    void onTransactTimeField(long transactTime);

    void onSymbolField(AsciiFieldFlyweight symbol);

    // -- callback methods for any other OrderSingle fields specified in the dictionary
}
