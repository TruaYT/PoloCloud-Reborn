package de.polocloud.wrapper;

import de.polocloud.api.CloudAPI;
import de.polocloud.api.CloudAPIType;
import de.polocloud.api.groups.GroupManager;
import de.polocloud.api.json.Document;
import de.polocloud.api.logger.Logger;
import de.polocloud.api.network.packet.ResponsePacket;
import de.polocloud.api.network.packet.init.CacheInitPacket;
import de.polocloud.api.network.packet.service.ServiceMemoryRequest;
import de.polocloud.api.player.PlayerManager;
import de.polocloud.api.service.CloudService;
import de.polocloud.api.service.ServiceManager;
import de.polocloud.wrapper.group.WrapperGroupManager;
import de.polocloud.wrapper.loader.ApplicationExternalClassLoader;
import de.polocloud.wrapper.logger.WrapperLogger;
import de.polocloud.wrapper.network.WrapperClient;
import de.polocloud.wrapper.player.CloudPlayerManager;
import de.polocloud.wrapper.service.WrapperServiceManager;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;

public final class Wrapper extends CloudAPI {

    private static Instrumentation instrumentation;

    public static void premain(final String s, final Instrumentation instrumentation) {
        Wrapper.instrumentation = instrumentation;
    }

    public static void main(String[] args) {
        try {
            final var wrapper = new Wrapper();

            var cacheInitialized = new AtomicBoolean(false);
            wrapper.getPacketHandler().registerPacketListener(CacheInitPacket.class, (channelHandlerContext, packet) -> cacheInitialized.set(true));

            final var arguments = new ArrayList<>(Arrays.asList(args));
            final var main = arguments.remove(0);
            final var applicationFile = Paths.get(arguments.remove(0));

            /**
             * Credits to @CloudNetServices
             * GitHub: https://github.com/CloudNetService/CloudNet-v3
             */
            ClassLoader loader = ClassLoader.getSystemClassLoader();

            var classLoader = ClassLoader.getSystemClassLoader();
            if (Boolean.parseBoolean(arguments.remove(0))) {
                classLoader = new ApplicationExternalClassLoader().addUrl(Paths.get(arguments.remove(0)));
                try (JarInputStream stream = new JarInputStream(Files.newInputStream(applicationFile))) {
                    JarEntry entry;
                    while ((entry = stream.getNextJarEntry()) != null) {
                        // only resolve class files
                        if (!entry.isDirectory() && entry.getName().endsWith(".class")) {
                            // canonicalize the class name
                            String className = entry.getName().replace('/', '.').replace(".class", "");
                            // load the class
                            try {
                                Class.forName(className, false, loader);
                            } catch (Throwable ignored) {
                                // ignore
                            }
                        }
                    }
                }

/*
                classLoader = new ApplicationExternalClassLoader().addUrl(Paths.get(arguments.remove(0)));
                try (final var jarInputStream = new JarInputStream(Files.newInputStream(applicationFile))) {
                    JarEntry jarEntry;
                    while ((jarEntry = jarInputStream.getNextJarEntry()) != null) {
                        if (jarEntry.getName().endsWith(".class")) {
                            Class.forName(jarEntry.getName().replace('/', '.').replace(".class", ""), false, classLoader);
                        }
                    }
                }
 */
            }

            instrumentation.appendToSystemClassLoaderSearch(new JarFile(applicationFile.toFile()));
            final var mainClass = Class.forName(main, true, classLoader);
            final var thread = new Thread(() -> {
                try {
                    mainClass.getMethod("main", String[].class).invoke(null, (Object) arguments.toArray(new String[0]));
                } catch (Exception exception) {
                    exception.printStackTrace();
                }
            }, "PoloCloud-Service-Thread");
            thread.setContextClassLoader(classLoader);
            if (cacheInitialized.get()) {
                thread.start();
            } else {
                wrapper.getPacketHandler().registerPacketListener(CacheInitPacket.class,
                    (channelHandlerContext, packet) -> thread.start());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Wrapper instance;

    private final GroupManager groupManager;
    private final ServiceManager serviceManager;
    private final PlayerManager playerManager;
    private final WrapperClient client;

    public Wrapper() {
        super(CloudAPIType.SERVICE);

        instance = this;

        final var property = new Document(new File("property.json")).get(PropertyFile.class);

        this.logger = new WrapperLogger();
        this.groupManager = new WrapperGroupManager();
        this.serviceManager = new WrapperServiceManager(property);
        this.playerManager = new CloudPlayerManager();
        this.client = new WrapperClient(this.packetHandler, property.getService(), property.getHostname(), property.getPort());

        Runtime.getRuntime().addShutdownHook(new Thread(this::stop, "PoloCloud-Shutdown-Thread"));

        packetHandler.registerPacketListener(ResponsePacket.class, (channelHandlerContext, packet) -> {
            if (packet.getPacket() instanceof ServiceMemoryRequest memoryRequest) {
                memoryRequest.setMemory((int) (calcMemory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory())));
            }
            channelHandlerContext.channel().writeAndFlush(packet);
        });
    }

    public static Wrapper getInstance() {
        return instance;
    }

    private void stop() {
        this.client.close();
    }

    @Override
    public Logger getLogger() {
        return this.logger;
    }

    @Override
    public @NotNull GroupManager getGroupManager() {
        return this.groupManager;
    }

    @Override
    public @NotNull ServiceManager getServiceManager() {
        return this.serviceManager;
    }

    @Override
    public @NotNull PlayerManager getPlayerManager() {
        return this.playerManager;
    }

    public CloudService thisService() {
        return ((WrapperServiceManager) this.serviceManager).thisService();
    }

    public WrapperClient getClient() {
        return this.client;
    }

    private long calcMemory(final long memory) {
        return memory / 1024 / 1024;
    }

}
