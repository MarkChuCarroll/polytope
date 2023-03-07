/*
 * Copyright 2023 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.goodmath.polytope.repository.agents

import org.goodmath.polytope.repository.data.ArtifactVersion

data class Text(
    val content: List<String>
)

class TextAgent(): Agent<Text> {
    override val artifactType: String = "text"

    override fun fromBytes(content: ByteArray): Text {
        TODO("Not yet implemented")
    }

    override fun merge(ancestor: ArtifactVersion, source: ArtifactVersion, target: ArtifactVersion): MergeResult {
        TODO("Not yet implemented")
    }

    override fun toBytes(content: Text): ByteArray {
        TODO("Not yet implemented")
    }

}