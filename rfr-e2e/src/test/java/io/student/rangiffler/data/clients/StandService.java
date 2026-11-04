package io.student.rangiffler.data.clients;

/**
 * Один сервис стенда, готовность которого проверяется одним ограниченным по времени запросом.
 */
public interface StandService {

  boolean ready();

  String outage();
}
