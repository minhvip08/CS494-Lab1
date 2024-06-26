package org.racingarena.server.tcp.model;

import org.json.JSONObject;
import org.racingarena.server.tcp.model.Player;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.*;
import java.util.logging.Logger;

public class WaitingRoom {
    public static int MAX_PLAYER = 2;
    public static int MAX_SCORE = 4;
    public volatile Map<SelectionKey, Player> players;
    public volatile Logger logger;

    public volatile int maxScore = 0;
    public WaitingRoom(Logger logger) {
        this.logger = logger;
        players = new LinkedHashMap<>();
    }

    public Boolean isFull() {
        return players.size() == MAX_PLAYER;
    }

    public synchronized int getPlayerCount() {
        return players.size();
    }

    public synchronized Boolean addPlayer(Player player) {
        if (players.containsValue(player)) {
            logger.info("Player " + player.getName() + " cannot join the waiting room");
            return false;
        }

        if (isFull()){
            logger.info("Player " + player.getName() + " cannot join the waiting room");
//            delete the player
            player.closeTheClient();

            return false;
        }



        logger.info("One Client joined the waiting room");
        players.put(player.getKey(), player);

//        Set the index of the player
        player.index = players.size() - 1;

        return true;
    }

    public void removePlayer(Player player) {
        logger.info("Player " + player.getName() + " left the waiting room");
        players.remove(player.getKey());
    }

    public synchronized List<Player> getPlayerRegistered() {
        List<Player> registeredPlayers = new ArrayList<>();
        for (Player player : players.values()) {
            if (player.isRegistered()) {
                registeredPlayers.add(player);
            }
        }
        return registeredPlayers;
    }

    public synchronized void broadcastEndGame(JSONObject message) {
        for (Player player : players.values()) {
            if (player.isRegistered())
            {
                message.put("score", player.getScore());
                player.writeTheBuffer(message.toString());
            }
        }
    }

//    broadcast to all players who are registered and not eliminated
    public synchronized void broadcastRegisteredNotEliminatedPlayer(String message) {
        for (Player player : players.values()) {
            if (player.isRegistered() && !player.isEliminated()) {
                player.writeTheBuffer(message);
            }
        }
    }

    public synchronized void broadcastRegisteredPlayer(String message) {
        for (Player player : players.values()) {
            if (player.isRegistered()) {
                player.writeTheBuffer(message);
                logger.info("Broadcast to " + player.getName());
            }

        }
    }

    public synchronized PlayerExtra getHighestScorePlayer() {
        Player highestScorePlayer = null;
        int i = -1, j = 0;
        for (Player player : players.values()) {
            ++i;
            if (player.isEliminated())
                continue;
            if (highestScorePlayer == null || player.getScore() > highestScorePlayer.getScore() ||
                    (player.getScore() == highestScorePlayer.getScore() && (player.getTimestamp() != null && highestScorePlayer.getTimestamp() != null && player.getTimestamp().isBefore(highestScorePlayer.getTimestamp())))) {
                highestScorePlayer = player;
                j = i;
            }
        }
        if (highestScorePlayer != null)
            maxScore = highestScorePlayer.getScore();
        return new PlayerExtra(highestScorePlayer, j);
    }

////    Get all players who are registered and not eliminated, sorted by timestamp
//    public synchronized List<Player> getSortedPlayersByTimestamp() {
//        List<Player> sortedPlayers = new ArrayList<>(getReadyPlayers());
////        sort for timestamp, if timestamp is the same, sort by name
//        sortedPlayers.sort(Comparator.comparing(Player::getTimestamp).thenComparing(Player::getName));
//        return sortedPlayers;
//    }

//    If a player has null timestamp, it means the player has not answered the question yet
    public synchronized Player getFastestPlayer() {
        Player lowestTimestampPlayer = null;
        for (Player player : getReadyPlayers()) {
            if (lowestTimestampPlayer == null ||
                    (player.getTimestamp() != null && lowestTimestampPlayer.getTimestamp() != null && player.getTimestamp().isBefore(lowestTimestampPlayer.getTimestamp()))) {
                lowestTimestampPlayer = player;
            }
            else if (lowestTimestampPlayer.getTimestamp() == null && player.getTimestamp() != null){
                lowestTimestampPlayer = player;
            }
        }
        return lowestTimestampPlayer;
    }

//    Check all players are eliminated or not
    public synchronized Boolean isAllPlayersEliminated() {
        for (Player player : players.values()) {
            if (!player.isEliminated()) {
                return false;
            }
        }
        return true;
    }

// Get all players who are registered and not eliminated
    public synchronized List<Player> getReadyPlayers() {
        List<Player> readyPlayers = new ArrayList<>();
        for (Player player : players.values()) {
            if (player.isRegistered() && !player.isEliminated())
                readyPlayers.add(player);
        }
        return readyPlayers;
    }

    public synchronized List<Player> getRegisteredPlayers(){
        List<Player> registeredPlayers = new ArrayList<>();
        for (Player player : players.values()) {
            if (player.isRegistered())
                registeredPlayers.add(player);
        }
        return registeredPlayers;
    }

    public synchronized Player getPlayerByName(String name) {
        for (Player player : players.values()) {
            if (player.getName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    public synchronized Map<SelectionKey, Player> getPlayers() {
        return players;
    }

    public synchronized Boolean isPlayerRegistered(String name) {
        for (Player player : players.values()) {
            if (player.getName().isEmpty())
                return false;
            if ( player.getName().equals(name)) {
                return player.isRegistered();
            }
        }
        return false;
    }

    public synchronized Boolean isPlayerExist(String name) {
        for (Player player : players.values()) {
            if (player.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public synchronized void resetPlayers() {
        for (Player player : players.values()) {
            player.reset();
        }
    }

    public synchronized void resetRound() {
        for (Player player : players.values()) {
            player.resetRound();
        }
    }

    public synchronized void resetWaitingRoom() {
        maxScore = 0;
    }
//    public synchronized void closeOnePlayer(Player player) {
//        player.closeTheClient();
//    }


//    public void broadcastExcept(String message, Player player) {
//        for (Player p : players) {
//            if (p != player) {
//                p.writeTheBuffer(message);
//            }
//        }
//    }


}
