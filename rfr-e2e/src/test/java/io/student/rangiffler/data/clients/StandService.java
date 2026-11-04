package io.student.rangiffler.data.clients;

import java.time.Instant;

/**
 * Один сервис стенда, готовность которого проверяется запросом, ограниченным и своим таймаутом, и общим сроком
 * ожидания стенда: после срока сервис не опрашивается и считается неготовым.
 */
public interface StandService {

  boolean ready(Instant deadline);

  String outage();
}
