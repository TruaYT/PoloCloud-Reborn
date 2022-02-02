package de.bytemc.cloud.api.network.packets.services;

import de.bytemc.network.packets.IPacket;
import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor @Getter @NoArgsConstructor
public class ServiceRequestShutdownPacket implements IPacket {

    private String service;

    @Override
    public void read(ByteBuf byteBuf) {
        this.service = readString(byteBuf);
    }

    @Override
    public void write(ByteBuf byteBuf) {
        writeString(byteBuf, service);
    }
}
