package dev.colocated.litebansdiscordbridge.support;

import litebans.api.Entry;

/**
 * Builder for {@link Entry} fixtures. LiteBans' Entry is an abstract class with an
 * 18-argument constructor, so tests go through this instead of calling it directly.
 */
public final class TestEntry {

    public static final String PLAYER_UUID = "da56db05-3ca2-4614-8f83-1c6648958007";

    private long id = 230L;
    private String type = "mute";
    private String uuid = PLAYER_UUID;
    private String ip = "127.0.0.1";
    private String reason = "ignore";
    private String executorUuid = "94660722-20ea-40eb-b508-7cd4abd6fd23";
    private String executorName = "zxqld";
    private String removedByUuid = null;
    private String removedByName = null;
    private String removalReason = null;
    private long dateStart = 1_700_000_000_000L;
    private long dateEnd = 0L;
    private String serverScope = "global";
    private String serverOrigin = "rtp";
    private boolean silent = false;
    private boolean ipban = false;
    private boolean active = true;
    private long duration = 0L;
    private long remainingDuration = 0L;
    private String randomId = "4AC6DA";

    public static TestEntry builder() {
        return new TestEntry();
    }

    public TestEntry id(long id) { this.id = id; return this; }
    public TestEntry type(String type) { this.type = type; return this; }
    public TestEntry uuid(String uuid) { this.uuid = uuid; return this; }
    public TestEntry ip(String ip) { this.ip = ip; return this; }
    public TestEntry reason(String reason) { this.reason = reason; return this; }
    public TestEntry executorUuid(String uuid) { this.executorUuid = uuid; return this; }
    public TestEntry executorName(String name) { this.executorName = name; return this; }
    public TestEntry removedBy(String uuid, String name) {
        this.removedByUuid = uuid;
        this.removedByName = name;
        return this;
    }
    public TestEntry removalReason(String reason) { this.removalReason = reason; return this; }
    public TestEntry dateStart(long millis) { this.dateStart = millis; return this; }
    public TestEntry dateEnd(long millis) { this.dateEnd = millis; return this; }
    public TestEntry serverScope(String scope) { this.serverScope = scope; return this; }
    public TestEntry serverOrigin(String origin) { this.serverOrigin = origin; return this; }
    public TestEntry silent(boolean silent) { this.silent = silent; return this; }
    public TestEntry ipban(boolean ipban) { this.ipban = ipban; return this; }
    public TestEntry active(boolean active) { this.active = active; return this; }
    public TestEntry duration(long millis) { this.duration = millis; return this; }
    public TestEntry remainingDuration(long millis) { this.remainingDuration = millis; return this; }
    public TestEntry randomId(String randomId) { this.randomId = randomId; return this; }

    public Entry build() {
        return new StubEntry(this);
    }

    private static final class StubEntry extends Entry {
        private final TestEntry spec;

        StubEntry(TestEntry spec) {
            super(spec.id, spec.type, spec.uuid, spec.ip, spec.reason,
                spec.executorUuid, spec.executorName,
                spec.removedByUuid, spec.removedByName, spec.removalReason,
                spec.dateStart, spec.dateEnd, spec.serverScope, spec.serverOrigin,
                (byte) 0, spec.silent, spec.ipban, spec.active);
            this.spec = spec;
        }

        @Override public long getDuration() { return spec.duration; }
        @Override public String getDurationString() { return String.valueOf(spec.duration); }
        @Override public long getRemainingDuration(long now) { return spec.remainingDuration; }
        @Override public String getRemainingDurationString(long now) { return String.valueOf(spec.remainingDuration); }
        @Override public String getRandomID() { return spec.randomId; }
        @Override public boolean isExpired(long now) { return false; }
        @Override public boolean isPermanent() { return spec.duration <= 0; }
        @Override public int getTemplateID() { return 0; }
        @Override public String getTemplateName() { return null; }
        @Override public boolean hasTemplate() { return false; }
    }
}
