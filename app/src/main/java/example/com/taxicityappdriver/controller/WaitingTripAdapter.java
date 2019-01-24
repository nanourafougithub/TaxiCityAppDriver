package example.com.taxicityappdriver.controller;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ramotion.foldingcell.FoldingCell;

import java.util.List;

import example.com.taxicityappdriver.R;
import example.com.taxicityappdriver.model.entities.Trip;
import example.com.taxicityappdriver.model.interfaces.SimpleCallBack;

public class WaitingTripAdapter extends RecyclerView.Adapter<WaitingTripViewHolder> {


    private List<Trip> items;//List of all trip items.

    private final static String TAG = "WaitingTripAdapter";

    /**
     * Check if the Driver is Busy (The busyKey is not null)
     *
     * @return 'true' fi is busy else 'false'
     */

    public static boolean isBusyDriver() {
        return WaitingTripViewHolder.getBusyKey() != null;
    }

    /**
     * Constructor
     * @param listTrips List of all trip items.
     */
    public WaitingTripAdapter(List<Trip> listTrips) {
        items = listTrips;

    }

    @NonNull
    @Override
    public WaitingTripViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        Context context = viewGroup.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.cell_trip, viewGroup, false);

        //InitWaitingTripViewHolder
        WaitingTripViewHolder.setContext(context);
        return new WaitingTripViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final WaitingTripViewHolder viewHolder, final int position) {
        viewHolder.setTrip(items.get(position));

        //Set the Fold Listener (Maybe change position)
        final FoldingCell finalCell = (FoldingCell) viewHolder.itemView;
        viewHolder.setFoldListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!WaitingTripAdapter.isBusyDriver()) {
                    finalCell.post(new Runnable() {
                        @Override
                        public void run() {
                            finalCell.toggle(false);
                        }
                    });
                }

            }
        });

        viewHolder.setOnEndTripSimpleCallBack(new SimpleCallBack() {
            @Override
            public void execute() {
                //Delete from the list
                items.remove(position);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, items.size());
            }
        });
        viewHolder.init();


    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

}
