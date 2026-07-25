package com.classiiiai.app;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import java.io.IOException;

public class SimulationFragment extends Fragment {

    private MorphImageView ivMorphPreview;
    private TextView tvSelectImage;
    private TextView tvMorphValue;
    private SeekBar sbMorph;
    private Button btnReset;

    private ActivityResultLauncher<Intent> galleryLauncher;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_simulation, container, false);

        ivMorphPreview = view.findViewById(R.id.ivMorphPreview);
        tvSelectImage = view.findViewById(R.id.tvSelectImage);
        tvMorphValue = view.findViewById(R.id.tvMorphValue);
        sbMorph = view.findViewById(R.id.sbMorph);
        btnReset = view.findViewById(R.id.btnReset);

        setupGalleryLauncher();

        ivMorphPreview.setOnClickListener(v -> openGallery());
        
        sbMorph.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // max 200, center is 100
                float morphVal = (progress - 100) / 10f; // -10.0 to 10.0
                ivMorphPreview.setMorphAmount(morphVal);
                
                if (morphVal < 0) {
                    tvMorphValue.setText(String.format("Setback: %.1f mm", morphVal));
                    tvMorphValue.setTextColor(getResources().getColor(R.color.errorColor, null));
                } else if (morphVal > 0) {
                    tvMorphValue.setText(String.format("Advancement: +%.1f mm", morphVal));
                    tvMorphValue.setTextColor(getResources().getColor(R.color.greenColor, null));
                } else {
                    tvMorphValue.setText("Neutral (0mm)");
                    tvMorphValue.setTextColor(getResources().getColor(R.color.primaryColor, null));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        btnReset.setOnClickListener(v -> {
            sbMorph.setProgress(100);
            ivMorphPreview.setMorphAmount(0);
        });

        return view;
    }

    private void setupGalleryLauncher() {
        galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri selectedImage = result.getData().getData();
                    try {
                        Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), selectedImage);
                        ivMorphPreview.setImageBitmap(bitmap);
                        tvSelectImage.setVisibility(View.GONE);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        );
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        galleryLauncher.launch(intent);
    }
}
