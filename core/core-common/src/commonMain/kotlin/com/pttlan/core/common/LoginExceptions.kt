package com.pttlan.core.common

/** The room has a PIN and the one sent does not match. The app shows its own text for it. */
class RoomPinRejectedException : IllegalStateException("Room PIN rejected")

/** Too many logins: the per-address rate limit, or a room locked after wrong PINs (30.3). */
class TooManyAttemptsException : IllegalStateException("Too many login attempts")

/** The server did not answer the login in time, even after a second try. */
class ServerTimeoutException : IllegalStateException("Server did not answer the login in time")
