package com.simpleworkout.timer;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;

public class FragmentPresetCards extends Fragment {

    private static final String TAG = "FragmentPresetCards";

    LinearLayoutManager linearLayoutManager;
    RecyclerView recyclerView;

    private ArrayList<Preset> presetCards = new ArrayList<>();

    private MyAdapter adapter;

    public void addPresetCard(int position, Preset preset) {
        Log.d(TAG, "addPresetCard: preset='" + preset.toString() + "'");
        presetCards.add(position, preset);
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }

    public void deletePresetCard(int position) {
        presetCards.remove(position);
        if (adapter != null) {
            adapter.notifyItemRemoved(position + 1);
            adapter.notifyItemRangeChanged(position + 1, adapter.getItemCount());
        }
    }

    public int getPresetCount() {
        return presetCards.size();
    }

    public boolean presetCardExists(Preset preset) {
        return presetCards.contains(preset);
    }

    public void showPresetCard(Preset preset) {
        int position = presetCards.indexOf(preset);
        linearLayoutManager.scrollToPosition(position + 1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        adapter = new MyAdapter();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");

        linearLayoutManager = new LinearLayoutManager(getActivity());
        linearLayoutManager.setOrientation(LinearLayoutManager.HORIZONTAL);

        View view = inflater.inflate(R.layout.fragment_horizontal_preset_cards, container, false);
        recyclerView = (RecyclerView) view.findViewById(R.id.cardView);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);

        Log.d(TAG, "onCreateView: adapter itemCount=" + adapter.getItemCount());

        recyclerView.invalidate();
        recyclerView.setLayoutManager(linearLayoutManager);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
    }


    public class MyAdapter extends RecyclerView.Adapter {

        @Override
        public int getItemViewType(int position) {
            return (position != 0)? 1 : 0;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == 1) {
                return new PresetViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.preset_card_view, parent, false));
            } else {
                return new AddPresetViewHolder(LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.preset_add_card_view, parent, false));
            }
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (holder.getItemViewType() == 1) {
                PresetViewHolder presetViewHolder = (PresetViewHolder)holder;
                presetViewHolder.textViewCardTimer.setText(presetCards.get(position - 1).getTimerString());
                presetViewHolder.textViewCardSets.setText(presetCards.get(position - 1).getSetsString());
                Log.d(TAG, "onBindViewHolder: position=" + position);
            }
        }

        @Override
        public int getItemCount() {
            return presetCards.size() + 1;
        }
    }

    public class PresetViewHolder extends RecyclerView.ViewHolder {

        TextView textViewCardTimer, textViewCardSets;
        private ImageButton imageButtonCard;

        private void inputPreset(int position) {
            ((MainActivity)getActivity()).inputPreset(position);
        }

        PresetViewHolder(View view) {
            super(view);
            textViewCardTimer = (TextView) view.findViewById(R.id.textViewCardTimer);
            textViewCardSets = (TextView) view.findViewById(R.id.textViewCardSets);
            imageButtonCard = (ImageButton) view.findViewById(R.id.imageButtonCard);

            imageButtonCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int position = getAdapterPosition() - 1;
                    Log.d(TAG, "OnClick: delete preset position=" + position);
                    deletePresetCard(position);
                    ((MainActivity)getActivity()).deletePreset(position);
                }
            });

            textViewCardTimer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    inputPreset(getAdapterPosition() - 1);
                }
            });

            textViewCardSets.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    inputPreset(getAdapterPosition() - 1);
                }
            });
        }
    }

    public class AddPresetViewHolder extends RecyclerView.ViewHolder {

        private ImageButton imageButtonCard;

        AddPresetViewHolder(View view) {
            super(view);
            imageButtonCard = (ImageButton) view.findViewById(R.id.imageButtonAddCard);

            imageButtonCard.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d(TAG, "OnClick: add preset");
                    ((MainActivity)getActivity()).addPreset();
                }
            });
        }
    }
}


