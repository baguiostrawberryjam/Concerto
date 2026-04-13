package com.example.concerto.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.concerto.R;
import com.example.concerto.models.HostedConcerto;
import java.util.ArrayList;
import java.util.List;

public class HostedConcertoAdapter extends RecyclerView.Adapter<HostedConcertoAdapter.ViewHolder> {

    private List<HostedConcerto> concertos = new ArrayList<>();
    private final OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(String concerto, boolean isActive);
    }

    public HostedConcertoAdapter(OnItemClickListener listener) {
        this.listener = listener;
    }

    // FIXED: Null-safe setter
    public void setConcertos(List<HostedConcerto> concertos) {
        this.concertos = concertos != null ? concertos : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hosted_concerto, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HostedConcerto concerto = concertos.get(position);
        holder.tvConcertoPin.setText("Room PIN: " + concerto.pin);

        boolean isActive = "active".equals(concerto.status);
        holder.tvConcertoStatus.setText(isActive ? "Active" : "Session Ended");
        holder.tvConcertoStatus.setTextColor(isActive ? android.graphics.Color.parseColor("#7C72E0") : android.graphics.Color.parseColor("#AAAAAA"));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(concerto.pin, isActive);
            }
        });
    }

    @Override
    public int getItemCount() {
        return concertos.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvConcertoPin;
        TextView tvConcertoStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvConcertoPin = itemView.findViewById(R.id.tvConcertoPin);
            tvConcertoStatus = itemView.findViewById(R.id.tvConcertoStatus);
        }
    }
}