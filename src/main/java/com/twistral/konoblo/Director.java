// Copyright 2025 Oğuzhan Topaloğlu
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.


package com.twistral.konoblo;

import java.util.function.Supplier;


class Director {
    public final DirectorType type;
    public final Supplier<String> nextIDSupplier;

    Director(DirectorType type, Supplier<String> nextIDSupplier) {
        this.nextIDSupplier = nextIDSupplier;
        this.type = type;
    }

    static enum DirectorType {
        EXIT, BACK, NEXT, SEP_INT, SEP_STR
    }
}