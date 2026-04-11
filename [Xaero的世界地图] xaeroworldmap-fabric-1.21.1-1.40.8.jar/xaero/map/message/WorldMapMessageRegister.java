package xaero.map.message;

import java.util.function.BiConsumer;
import xaero.lib.common.packet.IPacketHandler;
import xaero.map.message.basic.ClientboundRulesPacket;
import xaero.map.message.basic.HandshakePacket;
import xaero.map.message.tracker.ClientboundPlayerTrackerResetPacket;
import xaero.map.message.tracker.ClientboundTrackedPlayerPacket;
import xaero.map.server.level.LevelMapProperties;

public class WorldMapMessageRegister {
   public void register(IPacketHandler messageHandler) {
      messageHandler.register(0, LevelMapProperties.class, LevelMapProperties::write, LevelMapProperties::read, (BiConsumer)null, new LevelMapPropertiesConsumer());
      messageHandler.register(1, HandshakePacket.class, HandshakePacket::write, HandshakePacket::read, new HandshakePacket.ServerHandler(), new HandshakePacket.ClientHandler());
      messageHandler.register(2, ClientboundTrackedPlayerPacket.class, ClientboundTrackedPlayerPacket::write, ClientboundTrackedPlayerPacket::read, (BiConsumer)null, new ClientboundTrackedPlayerPacket.Handler());
      messageHandler.register(3, ClientboundPlayerTrackerResetPacket.class, ClientboundPlayerTrackerResetPacket::write, ClientboundPlayerTrackerResetPacket::read, (BiConsumer)null, new ClientboundPlayerTrackerResetPacket.Handler());
      messageHandler.register(4, ClientboundRulesPacket.class, ClientboundRulesPacket::write, ClientboundRulesPacket::read, (BiConsumer)null, new ClientboundRulesPacket.ClientHandler());
   }
}
