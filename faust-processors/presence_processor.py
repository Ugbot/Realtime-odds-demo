import faust
import data_models

app = faust.App(
    'presence-aggragation',
    broker='kafka://localhost:9092',
    value_serializer='raw',
)

presence_topic = app.topic('presence_events',value_type=PresenceEvent)

presence_table = app.Table('presence_set',default=PresenceEvent)

@app.agent(presence_topic)
async def build_presence_set(presence):
    async for change_event in presence:
        if change_event.id in presence_table:
            if change_event.timestamp > presence_table[change_event.id].timestamp:
                presence_table[change_event.id] = change_event
                pass
            pass
        else:
            presence_table[change_event.id] = change_event
            pass
        pass
    pass





