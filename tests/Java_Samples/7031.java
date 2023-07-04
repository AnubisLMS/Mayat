package coopnetserver.protocol.out;

import coopnetserver.Globals;
import coopnetserver.data.channel.Channel;
import coopnetserver.data.channel.ChannelData;
import coopnetserver.data.connection.Connection;
import coopnetserver.data.player.Player;
import coopnetserver.data.room.Room;
import coopnetserver.enums.ContactListElementTypes;
import coopnetserver.enums.ServerProtocolCommands;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

public class Protocol {

    public static final String INFORMATION_DELIMITER = "꬗";

    public static final String MESSAGE_DELIMITER = "ꬄ";

    public static byte[] ENCODED_MESSAGE_DELIMITER;

    public static final String HEARTBEAT = "♥";

    static {
        try {
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            ENCODED_MESSAGE_DELIMITER = encoder.encode(CharBuffer.wrap(MESSAGE_DELIMITER)).array();
        } catch (CharacterCodingException ex) {
            ex.printStackTrace();
        }
    }

    public static String encodeIPMap(Map<String, String> source) {
        StringBuilder sb = new StringBuilder();
        for (String key : source.keySet()) {
            sb.append(key + "=" + source.get(key) + ";");
        }
        return sb.toString();
    }

    public static Map<String, String> decodeIPMap(String input) {
        Map<String, String> map = new HashMap<String, String>();
        String[] entries = input.split(";");
        for (String entry : entries) {
            String[] pair = entry.split("=");
            map.put(pair[0], pair[1]);
        }
        return map;
    }

    public static void sendProtocolVersion(Connection to) {
        new Message(to, ServerProtocolCommands.COMPATIBILITY_VERSION, Globals.getCompatibilityVersion());
    }

    public static void acknowledgeContactRequest(Connection to, Player sender, String contactName) {
        new Message(to, sender, ServerProtocolCommands.CONTACT_REQUEST_ACKNOWLEDGE, contactName);
    }

    public static void acknowledgeContactAccept(Connection to, Player sender, String contactName) {
        new Message(to, sender, ServerProtocolCommands.CONTACT_ACCEPT_ACKNOWLEDGE, contactName);
    }

    public static void acknowledgeContactRefuse(Connection to, Player sender, String contactName) {
        new Message(to, sender, ServerProtocolCommands.CONTACT_REFUSE_ACKNOWLEDGE, contactName);
    }

    public static void acknowledgeContactRemove(Connection to, Player sender, String contactName) {
        new Message(to, sender, ServerProtocolCommands.CONTACT_REMOVE_ACKNOWLEDGE, contactName);
    }

    public static void acknowledgeContactMove(Connection to, Player sender, String contactName, String groupName) {
        String[] info = { contactName, groupName };
        new Message(to, sender, ServerProtocolCommands.CONTACT_MOVE_ACKNOWLEDGE, info);
    }

    public static void acknowledgeGroupCreate(Connection to, Player sender, String groupName) {
        new Message(to, sender, ServerProtocolCommands.GROUP_CREATE_ACKNOWLEDGE, groupName);
    }

    public static void acknowledgeGroupDelete(Connection to, Player sender, String groupName) {
        new Message(to, sender, ServerProtocolCommands.GROUP_DELETE_ACKNOWLEDGE, groupName);
    }

    public static void acknowledgeGroupRename(Connection to, Player sender, String groupName, String newName) {
        String[] info = { groupName, newName };
        new Message(to, sender, ServerProtocolCommands.GROUP_RENAME_ACKNOWLEDGE, info);
    }

    public static void sendContactRequest(Connection to, Player sender) {
        new Message(to, sender, ServerProtocolCommands.CONTACT_REQUESTED, sender.getLoginName());
    }

    public static void sendContactStatus(Connection to, Player sender, ContactListElementTypes status) {
        String[] info = { sender.getLoginName(), String.valueOf(status.ordinal()) };
        new Message(to, sender, ServerProtocolCommands.SET_CONTACTSTATUS, info);
    }

    public static void sendRequestAcceptNotification(Connection to, Player sender) {
        new Message(to, sender, ServerProtocolCommands.ACCEPTED_CONTACT_REQUEST, sender.getLoginName());
    }

    public static void sendRequestRefuseNotification(Connection to, Player sender) {
        new Message(to, sender, ServerProtocolCommands.REFUSED_CONTACT_REQUEST, sender.getLoginName());
    }

    public static void sendContactData(Connection to, String[] data) throws SQLException {
        new Message(to, ServerProtocolCommands.CONTACT_LIST, data);
    }

    public static void sendIP(Connection to, String IP) {
        new Message(to, ServerProtocolCommands.YOUR_IP_IS, IP);
    }

    public static void sendContactData(Player to, boolean sendOffline) throws SQLException {
        to.sendContactData(sendOffline);
    }

    public static void sendMuteBanData(Player to) throws SQLException {
        to.sendMuteBanData();
    }

    public static void sendMuteBanData(Connection to, String[] data) throws SQLException {
        new Message(to, ServerProtocolCommands.MUTE_BAN_LIST, data);
    }

    public static void sendFile(Connection to, Player from, String filename, String size, String ip, String port) {
        String[] info = { from.getLoginName(), size, filename, ip, port };
        new Message(to, from, ServerProtocolCommands.SENDING_FILE, info);
    }

    public static void acceptFile(Connection to, Player from, String filename, String ip, String port, String firstByte) {
        String[] info = { ip, from.getLoginName(), filename, port, firstByte };
        new Message(to, from, ServerProtocolCommands.ACCEPTED_FILE, info);
    }

    public static void refuseFile(Connection to, Player from, String filename) {
        String[] info = { from.getLoginName(), filename };
        new Message(to, from, ServerProtocolCommands.REFUSED_FILE, info);
    }

    public static void cancelFile(Connection to, Player from, String filename) {
        String[] info = { from.getLoginName(), filename };
        new Message(to, from, ServerProtocolCommands.CANCELED_FILE, info);
    }

    public static void requestPassword(Connection to, String roomID) {
        new Message(to, ServerProtocolCommands.REQUEST_PASSWORD, roomID);
    }

    public static void wrongPasswordAtRoomJoin(Connection to) {
        new Message(to, ServerProtocolCommands.WRONG_ROOM_PASSWORD);
    }

    public static void sendCrippledModeNotification(Connection to) {
        new Message(to, ServerProtocolCommands.CRIPPLED_SERVER_MODE);
    }

    /**
     * send a public message to the channel
     */
    public static void mainChat(Channel to, Player from, String message) {
        String[] info = { to.ID, from.getLoginName(), message };
        for (Player connection : to.getPlayersInChannel()) {
            new Message(connection.getConnection(), from, ServerProtocolCommands.CHAT_MAIN, info);
        }
    }

    /**
     * send a message to the current room
     */
    public static void roomChat(Room to, Player from, String message) {
        String[] info = { ((from == null) ? "" : from.getLoginName()), message };
        for (Player connection : to.getPlayers()) {
            new Message(connection.getConnection(), from, ServerProtocolCommands.CHAT_ROOM, info);
        }
    }

    public static void sendInstantLaunchCommand(Connection to, Room room) {
        String[] info1 = { room.parent.ID, room.getModIndex(), encodeIPMap(room.getInterfaceIPs()), room.getPassword() };
        String[] info2 = new String[info1.length + 2 * (room.getSettings().size())];
        System.arraycopy(info1, 0, info2, 0, info1.length);
        Iterator<Entry<String, String>> iter = room.getSettings().iterator();
        int index = info1.length;
        while (iter.hasNext()) {
            Entry<String, String> e = iter.next();
            info2[index] = e.getKey();
            info2[index + 1] = e.getValue();
            index += 2;
        }
        new Message(to, ServerProtocolCommands.INSTANT_LAUNCH, info2);
    }

    public static void sendSetting(Connection to, String name, String value) {
        String[] info = { name, value };
        new Message(to, ServerProtocolCommands.SET_GAMESETTING, info);
    }

    public static void turnAroundTransfer(Connection to, Player from, String filename) {
        String[] info = { from.getLoginName(), filename };
        new Message(to, from, ServerProtocolCommands.TURN_AROUND_FILE, info);
    }

    public static void joinChannel(Connection to, Channel channel) {
        Player[] players = channel.getPlayersInChannel();
        String[] data = new String[players.length + 1];
        data[0] = channel.ID;
        for (int i = 0; i < players.length; ++i) {
            data[i + 1] = players[i].getLoginName();
        }
        new Message(to, ServerProtocolCommands.JOIN_CHANNEL, data);
    }

    /**
     * remove player from the channel (player list)
     */
    public static void removePlayerFromChannel(Channel to, Player player) {
        String[] info = { to.ID, player.getLoginName() };
        for (Player p : to.getPlayersInChannel()) {
            new Message(p.getConnection(), ServerProtocolCommands.LEFT_CHANNEL, info);
        }
    }

    public static void addPlayerToChannel(Channel to, Player player) {
        String[] info = { to.ID, player.getLoginName() };
        for (Player p : to.getPlayersInChannel()) {
            new Message(p.getConnection(), ServerProtocolCommands.ADD_TO_PLAYERS, info);
        }
    }

    /**
     * send a private message to someone
     */
    public static void privateMessage(Connection to, Player from, String message) {
        String[] info = { from.getLoginName(), message };
        new Message(to, from, ServerProtocolCommands.CHAT_PRIVATE, info);
    }

    public static void inviteUser(Connection to, Player from, Room room) {
        String[] info = { from.getLoginName(), String.valueOf(room.getID()), String.valueOf(room.parent.ID) };
        new Message(to, from, ServerProtocolCommands.ROOM_INVITE, info);
    }

    public static void gameClosed(String channelID, Player roomHost, Player[] recipients) {
        String[] info = { channelID, roomHost.getLoginName() };
        for (Player p : recipients) {
            new Message(p.getConnection(), ServerProtocolCommands.GAME_CLOSED, info);
        }
    }

    public static void nudge(Connection to, Player from) {
        new Message(to, from, ServerProtocolCommands.NUDGE, from.getLoginName());
    }

    /**
     * message to the client to close the room (he was kicked)
     */
    public static void kick(Connection to) {
        new Message(to, ServerProtocolCommands.KICKED);
    }

    /**
     * message to remove someone from the member list
     */
    public static void removePlayerFromRoomInList(Channel to, Player host, Player removed) {
        String[] info = { to.ID, host.getLoginName(), removed.getLoginName() };
        for (Player p : to.getPlayersInChannel()) {
            new Message(p.getConnection(), ServerProtocolCommands.LEFT_ROOM, info);
        }
    }

    public static void sendServerShuttingDown() {
        for (Channel ch : ChannelData.getChannels().values()) {
            for (Player p : ch.getPlayersInChannel()) {
                new Message(p.getConnection(), ServerProtocolCommands.SERVER_SHUTTING_DOWN);
            }
        }
    }

    public static void sendNoSuchPlayer(Connection to) {
        new Message(to, ServerProtocolCommands.ECHO_NO_SUCH_PLAYER);
    }

    /**
     * reply that the player was added to the banlist
     */
    public static void confirmBan(Connection to, String bannedone) {
        new Message(to, ServerProtocolCommands.ECHO_BANNED, bannedone);
    }

    public static void confirmUnBan(Connection to, String bannedone) {
        new Message(to, ServerProtocolCommands.ECHO_UNBANNED, bannedone);
    }

    public static void confirmMute(Connection to, String mutedone) {
        new Message(to, ServerProtocolCommands.ECHO_MUTED, mutedone);
    }

    public static void confirmUnMute(Connection to, String mutedone) {
        new Message(to, ServerProtocolCommands.ECHO_UNMUTED, mutedone);
    }

    public static void sendReadyStatus(Room to, Player who) {
        if (who.isReady()) {
            for (Player p : to.getPlayers()) {
                new Message(p.getConnection(), ServerProtocolCommands.READY_STATUS, who.getLoginName());
            }
        } else {
            for (Player p : to.getPlayers()) {
                new Message(p.getConnection(), ServerProtocolCommands.NOT_READY_STATUS, who.getLoginName());
            }
        }
    }

    public static void sendReadyStatus(Connection to, Player who) {
        if (who.isReady()) {
            new Message(to, ServerProtocolCommands.READY_STATUS, who.getLoginName());
        } else {
            new Message(to, ServerProtocolCommands.NOT_READY_STATUS, who.getLoginName());
        }
    }

    public static void sendRoomPlayingStatus(Connection to, Player player) {
        new Message(to, ServerProtocolCommands.ROOM_PLAYING_STATUS, player.getLoginName());
    }

    public static void sendRoomPlayingStatusToRoom(Room to, Player who) {
        for (Player connection : to.getPlayers()) {
            new Message(connection.getConnection(), ServerProtocolCommands.ROOM_PLAYING_STATUS, who.getLoginName());
        }
    }

    public static void sendChannelPlayingStatus(Channel to, Player player) {
        String[] info = { to.ID, player.getLoginName() };
        for (Player p : to.getPlayersInChannel()) {
            new Message(p.getConnection(), ServerProtocolCommands.CHANNEL_PLAYING_STATUS, info);
        }
    }

    public static void editProfile(Player player) {
        String[] info = { player.getLoginName(), player.getIngameName(), player.getEmail(), player.getCountry(), player.getWebsite() };
        new Message(player.getConnection(), ServerProtocolCommands.EDIT_PROFILE, info);
    }

    public static void showProfile(Connection to, String loginname, String ingamename, String country, String website) {
        String[] info = { loginname, ingamename, country, website };
        new Message(to, ServerProtocolCommands.SHOW_PROFILE, info);
    }

    public static void updateName(Connection to, String oldname, String newname) {
        String[] info = { oldname, newname };
        new Message(to, ServerProtocolCommands.UPDATE_PLAYERNAME, info);
    }

    public static void errorYouAreBanned(Connection to) {
        new Message(to, ServerProtocolCommands.ERROR_YOU_ARE_BANNED);
    }

    public static void errorRoomIsFull(Connection to) {
        new Message(to, ServerProtocolCommands.ERROR_ROOM_IS_FULL);
    }

    public static void errorRoomDoesNotExist(Connection to) {
        new Message(to, ServerProtocolCommands.ERROR_ROOM_DOES_NOT_EXIST);
    }

    public static void errorLoginnameIsAlreadyUsed(Connection to) {
        new Message(to, ServerProtocolCommands.ERROR_LOGINNAME_IS_ALREADY_USED);
    }

    public static void errorIncorrectPassword(Connection to) {
        new Message(to, ServerProtocolCommands.ERROR_INCORRECT_PASSWORD);
    }

    public static void confirmProfileChange(Connection to) {
        new Message(to, ServerProtocolCommands.PROFILE_SAVED);
    }

    public static void setInGameName(Player player) {
        new Message(player.getConnection(), ServerProtocolCommands.INGAMENAME, player.getIngameName());
    }

    public static void confirmPasswordChange(Connection to) {
        new Message(to, ServerProtocolCommands.PASSWORD_CHANGED);
    }

    public static void sendJoinNotification(Channel ch, Connection to, Player host, Player joiner) {
        String[] info = { ch.ID, host.getLoginName(), joiner.getLoginName() };
        new Message(to, ServerProtocolCommands.JOINED_ROOM, info);
    }

    public static void sendJoinNotification(Channel to, Player host, Player joiner) {
        for (Player p : to.getPlayersInChannel()) {
            sendJoinNotification(to, p.getConnection(), host, joiner);
        }
    }

    public static void confirmLogin(Connection to, String loginName) {
        new Message(to, ServerProtocolCommands.OK_LOGIN, loginName);
    }

    public static void confirmRegister(Connection to) {
        new Message(to, ServerProtocolCommands.OK_REGISTER);
    }

    public static void nameAlreadyUsed(Connection to) {
        new Message(to, ServerProtocolCommands.LOGINNAME_IN_USE);
    }

    public static void wrongLogin(Connection to) {
        new Message(to, ServerProtocolCommands.LOGIN_INCORRECT);
    }

    public static void joinRoom(Connection to, Channel on, Room room) {
        String[] info = { on.ID, encodeIPMap(room.getInterfaceIPs()), String.valueOf(room.getLimit()), String.valueOf(room.getModIndex()), room.getHost().getLoginName(), room.getName(), String.valueOf(room.getID()), room.getPassword(), String.valueOf(room.isSearchEnabled()) };
        Player[] players = room.getPlayers();
        String[] data = new String[info.length + players.length];
        System.arraycopy(info, 0, data, 0, info.length);
        for (int i = 0; i < players.length; ++i) {
            data[i + info.length] = players[i].getLoginName();
        }
        new Message(to, ServerProtocolCommands.JOIN_ROOM, data);
    }

    public static void addMemberToRoom(Connection to, Player newmember) {
        new Message(to, ServerProtocolCommands.ADD_MEMBER_TO_ROOM, newmember.getLoginName());
    }

    public static void addMemberToRoom(Room to, Player newmember) {
        for (Player p : to.getPlayers()) {
            addMemberToRoom(p.getConnection(), newmember);
        }
    }

    public static void createRoom(Connection to, Channel on, Room room) {
        String[] info = { on.ID, String.valueOf(room.getLimit()), room.getModIndex(), room.getName(), String.valueOf(room.getID()), room.getPassword(), String.valueOf(room.isSearchEnabled()) };
        new Message(to, ServerProtocolCommands.CREATE_ROOM, info);
    }

    public static void addNewRoom(Connection to, Channel on, Room room) {
        String[] info = { on.ID, room.getName(), room.getHost().getLoginName(), String.valueOf(room.getLimit()), String.valueOf(room.getType()), room.getModIndex() };
        new Message(to, ServerProtocolCommands.ADD_ROOM, info);
    }

    public static void addNewRoom(Channel to, Room room) {
        for (Player p : to.getPlayersInChannel()) {
            addNewRoom(p.getConnection(), to, room);
        }
    }

    public static void launch(Player to) {
        new Message(to.getConnection(), ServerProtocolCommands.LAUNCH);
    }

    public static void leaveRoom(Connection to) {
        new Message(to, ServerProtocolCommands.LEAVE_ROOM);
    }

    public static void setAwayStatus(Player to, Player player) {
        new Message(to.getConnection(), ServerProtocolCommands.SETAWAYSTATUS, player.getLoginName());
    }

    public static void unSetAwayStatus(Player to, Player player) {
        new Message(to.getConnection(), ServerProtocolCommands.UNSETAWAYSTATUS, player.getLoginName());
    }

    public static void closeRoom(Room to, Channel on) {
        String[] info = { on.ID, to.getHost().getLoginName() };
        for (Player p : to.getPlayers()) {
            new Message(p.getConnection(), ServerProtocolCommands.CLOSE_ROOM, info);
        }
    }

    public static void removeRoom(Channel to, Player host) {
        String[] info = { to.ID, host.getLoginName() };
        for (Player p : to.getPlayersInChannel()) {
            new Message(p.getConnection(), ServerProtocolCommands.REMOVE_ROOM, info);
        }
    }

    public static void removeMemberFromRoom(Room to, Player player) {
        for (Player p : to.getPlayers()) {
            new Message(p.getConnection(), ServerProtocolCommands.REMOVE_MEMBER_FROM_ROOM, player.getLoginName());
        }
    }

    public static void sendWhisperToOfflineUser(Player to, String whisperTo) {
        new Message(to.getConnection(), ServerProtocolCommands.ERROR_WHISPER_TO_OFFLINE_USER, whisperTo);
    }

    public static void sendRemoveRequest(Player to, Player who) {
        new Message(to.getConnection(), ServerProtocolCommands.REMOVE_CONTACT_REQUEST, who.getLoginName());
    }

    public static void sendConnectionTestRequest(Player to, String[] info) {
        new Message(to.getConnection(), ServerProtocolCommands.CONNECTION_TEST_REQUEST, info);
    }
}
