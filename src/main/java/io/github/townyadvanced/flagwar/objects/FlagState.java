/*
 * Copyright (c) 2025 TownyAdvanced
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.townyadvanced.flagwar.objects;

public enum FlagState
    {
        preFlag, // flagging has not begun yet.
        flag, // main portion of the war; flagging is occurring now.
        ruined, // if a town loses a defense, it gets ruined, until it's reclaimed and turns extinct.

        extinct, // extinct means that the war info has nothing else to do or wait for. this is its period of invincibility, but it will not undergo any other action.
        // it can be said that the war is "over", but the war still exists on the YML and hashmap in order to prevent attacks until the configured time has elapsed.
    }
