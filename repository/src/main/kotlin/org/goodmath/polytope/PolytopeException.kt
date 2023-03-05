package org.goodmath.polytope

data class PolytopeException(
    val kind: Kind,
    val msg: String,
    override val cause: Throwable? = null): Exception("Error($kind): $msg", cause) {
    enum class Kind {
        Internal, InvalidParameter, Permission,
        NotFound, Conflict, Authentication
    }
}