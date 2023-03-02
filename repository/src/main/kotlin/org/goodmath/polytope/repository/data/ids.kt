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

package org.goodmath.polytope.repository.data

import org.bson.types.ObjectId
import java.util.*

data class ArtifactId(val id: ObjectId)
data class VersionId(val artifactId: ArtifactId, val versionId: ObjectId)
data class ContentId(val id: UUID, val category: String)
data class ChangeId(val id: ObjectId)
data class ChangeStepId(val change: ChangeId, val id: ObjectId)
