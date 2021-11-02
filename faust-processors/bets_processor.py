import faust
import data_models

app = faust.App(
    'bets-processor',
    broker='kafka://localhost:9092',
    value_serializer='raw',
)


