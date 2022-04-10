package vip.mannheim.radio;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class StationAdapter extends RecyclerView.Adapter<StationAdapter.NewsViewHolder> implements Filterable {


    Context mContext;
    List<StationItem> mData ;
    List<StationItem> mDataFiltered ;

    OnStationListener onStationListener;

    public StationAdapter(Context mContext, List<StationItem> mData, OnStationListener onStationListener) {
        this.mContext = mContext;
        this.mData = mData;
        this.mDataFiltered = mData;

        this.onStationListener = onStationListener;
    }

    @NonNull
    @Override
    public NewsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View layout;
        layout = LayoutInflater.from(mContext).inflate(R.layout.item_station, viewGroup,false);
        return new NewsViewHolder(layout, onStationListener);
    }

    @Override
    public void onBindViewHolder(@NonNull NewsViewHolder newsViewHolder, int position) {
        newsViewHolder.tv_title.setText(String.valueOf(position + 1));
        newsViewHolder.tv_content.setText(mDataFiltered.get(position).getContent());
    }

    @Override
    public int getItemCount() {
        return mDataFiltered.size();
    }

    @Override
    public Filter getFilter() {

        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence constraint) {

                String Key = constraint.toString();
                if (Key.isEmpty()) {
                    mDataFiltered = mData ;
                }
                else {
                    List<StationItem> lstFiltered = new ArrayList<>();
                    for (StationItem row : mData) {
                        if (row.getContent().toLowerCase().contains(Key.toLowerCase())){
                            lstFiltered.add(row);
                        }
                    }
                    mDataFiltered = lstFiltered;
                }
                FilterResults filterResults = new FilterResults();
                filterResults.values = mDataFiltered;
                return filterResults;
            }

            @Override
            protected void publishResults(CharSequence constraint, FilterResults results) {

                mDataFiltered = (List<StationItem>) results.values;
                notifyDataSetChanged();
            }
        };
    }

    public class NewsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        TextView tv_title, tv_content;
        FrameLayout container;
        OnStationListener onStationListener;
        public NewsViewHolder(@NonNull View itemView, OnStationListener onStationListener) {
            super(itemView);
            container = itemView.findViewById(R.id.container);
            tv_title = itemView.findViewById(R.id.tv_title);
            tv_content = itemView.findViewById(R.id.tv_description);

            this.onStationListener = onStationListener;
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View v) {
            onStationListener.onStationClick(getAdapterPosition(), v);

        }
    }
    public interface OnStationListener {
        void onStationClick(int position, View v);
    }
}
