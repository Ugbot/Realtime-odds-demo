import faust

class PresenceEvent(faust.Record):
    id: str
    clientId: str
    connectionId: str
    timestamp: int
    data: str
    action: int