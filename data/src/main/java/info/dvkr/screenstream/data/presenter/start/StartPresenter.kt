package info.dvkr.screenstream.data.presenter.start

import info.dvkr.screenstream.data.presenter.BasePresenter
import info.dvkr.screenstream.data.presenter.BaseView
import info.dvkr.screenstream.domain.eventbus.EventBus
import info.dvkr.screenstream.domain.globalstatus.GlobalStatus
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.actor
import kotlinx.coroutines.experimental.launch
import kotlin.coroutines.experimental.CoroutineContext

class StartPresenter(crtContext: CoroutineContext, eventBus: EventBus,
                     private val globalStatus: GlobalStatus) :
        BasePresenter<StartView, StartView.FromEvent>(crtContext, eventBus) {

    init {
        viewChannel = actor(crtContext, Channel.UNLIMITED) {
            for (fromEvent in this) when (fromEvent) {
                is StartView.FromEvent.CurrentInterfacesRequest ->
                    eventBus.send(EventBus.GlobalEvent.CurrentInterfacesRequest)

                is StartView.FromEvent.TryStartStream ->
                    if (globalStatus.isStreamRunning.get().not()) {
                        globalStatus.error.set(null)
                        notifyView(StartView.ToEvent.TryToStart())
                    }

                is StartView.FromEvent.StopStream ->
                    if (globalStatus.isStreamRunning.get())
                        eventBus.send(EventBus.GlobalEvent.StopStream)

                is StartView.FromEvent.AppExit ->
                    eventBus.send(EventBus.GlobalEvent.AppExit)

                is StartView.FromEvent.Error -> {
                    globalStatus.error.set(fromEvent.error)
                    notifyView(BaseView.BaseToEvent.Error(fromEvent.error))
                }

                is StartView.FromEvent.GetError ->
                    notifyView(BaseView.BaseToEvent.Error(globalStatus.error.get()))
            }
        }
    }

    fun attach(newView: StartView) {
        super.attach(newView) { globalEvent ->
            when (globalEvent) {
                is EventBus.GlobalEvent.StreamStatus ->
                    notifyView(StartView.ToEvent.OnStreamStartStop(globalStatus.isStreamRunning.get()))

                is EventBus.GlobalEvent.ResizeFactor ->
                    notifyView(StartView.ToEvent.ResizeFactor(globalEvent.value))

                is EventBus.GlobalEvent.EnablePin ->
                    notifyView(StartView.ToEvent.EnablePin(globalEvent.value))

                is EventBus.GlobalEvent.SetPin ->
                    notifyView(StartView.ToEvent.SetPin(globalEvent.value))

                is EventBus.GlobalEvent.CurrentClients ->
                    notifyView(StartView.ToEvent.CurrentClients(globalEvent.clientsList))

                is EventBus.GlobalEvent.CurrentInterfaces ->
                    notifyView(StartView.ToEvent.CurrentInterfaces(globalEvent.interfaceList))

                is EventBus.GlobalEvent.TrafficPoint ->
                    notifyView(StartView.ToEvent.TrafficPoint(globalEvent.trafficPoint))
            }
        }

        // Sending current stream status
        notifyView(StartView.ToEvent.StreamRunning(globalStatus.isStreamRunning.get()))

        // Requesting current clients
        launch(crtContext) { eventBus.send(EventBus.GlobalEvent.CurrentClientsRequest) }
    }
}