package com.aula.volta;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class CarouselAdapter extends RecyclerView.Adapter<CarouselAdapter.CtaHolder> {

    public interface OnRegisterClickListener {
        void onRegisterClick();
    }

    private final OnRegisterClickListener listener;

    public CarouselAdapter(OnRegisterClickListener listener) {
        this.listener = listener;
    }

    @Override
    public int getItemCount() {
        return 1;
    }

    @NonNull
    @Override
    public CtaHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new CtaHolder(inflater.inflate(R.layout.item_carousel_cta, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull CtaHolder holder, int position) {
        if (holder.registerButton != null) {
            holder.registerButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRegisterClick();
                }
            });
        }
    }

    static class CtaHolder extends RecyclerView.ViewHolder {
        final View registerButton;

        CtaHolder(@NonNull View itemView) {
            super(itemView);
            registerButton = itemView.findViewById(R.id.btnSlideRegister);
        }
    }
}
