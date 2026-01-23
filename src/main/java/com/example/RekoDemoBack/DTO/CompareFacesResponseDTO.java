package com.example.RekoDemoBack.DTO;

import lombok.Data;
import java.util.List;

@Data
public class CompareFacesResponseDTO {
    private String status;
    private String message;
    private Float similarityScore;
    private Boolean isMatch;
    private String confidenceLevel;
    private Integer faceMatchesCount;
    private List<FaceMatchDTO> faceMatches;
    private FaceDetailsDTO sourceFaceDetails;
    private FaceDetailsDTO targetFaceDetails;

    @Data
    public static class FaceMatchDTO {
        private Float similarity;
        private BoundingBoxDTO boundingBox;
        private Float poseRoll;
        private Float poseYaw;
        private Float posePitch;
        private Float qualityBrightness;
        private Float qualitySharpness;
    }

    @Data
    public static class BoundingBoxDTO {
        private Float width;
        private Float height;
        private Float left;
        private Float top;
    }

    @Data
    public static class FaceDetailsDTO {
        private BoundingBoxDTO boundingBox;
        private AgeRangeDTO ageRange;
        private String gender;
        private List<EmotionDTO> emotions;
        private Boolean hasBeard;
        private Boolean hasMustache;
        private Boolean hasSunglasses;
        private Boolean hasEyesOpen;
        private Boolean hasMouthOpen;
    }

    @Data
    public static class AgeRangeDTO {
        private Integer low;
        private Integer high;
    }

    @Data
    public static class EmotionDTO {
        private String type;
        private Float confidence;
    }
}