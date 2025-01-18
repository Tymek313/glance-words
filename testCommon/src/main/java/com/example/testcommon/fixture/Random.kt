package com.example.testcommon.fixture

import java.time.Instant
import java.util.UUID
import kotlin.random.Random

fun randomString() = UUID.randomUUID().toString()

fun randomInt() = Random.nextInt()

fun randomInstant(): Instant = Instant.ofEpochSecond(randomEpochSeconds())

fun randomEpochSeconds() = Random.nextLong(Instant.MIN.epochSecond, Instant.MAX.epochSecond)
