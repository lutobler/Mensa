package ch.famoser.mensa.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ch.famoser.mensa.adapters.LocationAdapter
import ch.famoser.mensa.events.MensaMenuUpdatedEvent
import ch.famoser.mensa.events.RefreshMensaFinishedEvent
import ch.famoser.mensa.events.RefreshMensaProgressEvent
import ch.famoser.mensa.events.RefreshMensaStartedEvent
import ch.famoser.mensa.repositories.LocationRepository
import ch.famoser.mensa.services.ProgressCollector
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.time.LocalDate
import android.os.Parcelable




/**
 * An activity representing a list of Pings. This activity
 * has different presentations for handset and tablet-size devices. On
 * handsets, the activity presents a list of items, which when touched,
 * lead to a [MensaActivity] representing
 * item details. On tablets, the activity presents the list of items and
 * item details side-by-side using two vertical panes.
 */
class MainActivity : AppCompatActivity() {

    companion object {
        var locationListScrollState: Parcelable? = null
    }

    /**
     * Whether or not the activity is in two-pane mode, i.e. running on a tablet
     * device.
     */
    private var twoPane: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(ch.famoser.mensa.R.layout.activity_main)

        if (details_container != null) {
            // The detail container view will be present only in the
            // large-screen layouts (res/values-w900dp).
            // If this view is present, then the
            // activity should be in two-pane mode.
            twoPane = true
        }

        this.refreshMensaEventProcessor = ProgressCollector(swipeContainer, downloadProgress)

        val locationRepository = LocationRepository.getInstance(assets)
        val locations = locationRepository.getLocations()

        val locationAdapter = LocationAdapter(this, twoPane, locations)
        location_list.adapter = locationAdapter
        this.locationListAdapter = locationAdapter

        if (locationListScrollState != null) {
            location_list.onRestoreInstanceState(locationListScrollState);
        }

        EventBus.getDefault().register(this)
        locationRepository.refresh(LocalDate.now())

        swipeContainer.setOnRefreshListener {
            locationRepository.refresh(LocalDate.now(), true)
        }
    }

    public override fun onPause() {
        // Save ListView locationListScrollState @ onPause
        locationListScrollState = location_list.onSaveInstanceState()
        super.onPause()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMensaMenuUpdatedEvent(event: MensaMenuUpdatedEvent) {
        locationListAdapter?.mensaMenusRefreshed(event.mensaId)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshMensaStartedEvent(event: RefreshMensaStartedEvent) {
        refreshMensaEventProcessor?.onStarted(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshMensaProgressEvent(event: RefreshMensaProgressEvent) {
        refreshMensaEventProcessor?.onProgress(event)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onRefreshMensaFinishedEvent(event: RefreshMensaFinishedEvent) {
        refreshMensaEventProcessor?.onFinished(event)
    }

    private var locationListAdapter: LocationAdapter? = null
    private var refreshMensaEventProcessor: ProgressCollector? = null

    public override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}