package com.zoopick.server.service;

import com.zoopick.server.dto.image.ImageUploadResult;
import com.zoopick.server.exception.BadRequestException;
import com.zoopick.server.exception.DataNotFoundException;
import com.zoopick.server.image.ImagePurpose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class ImageServiceTest {

    private ImageService imageService;

    // JUnit5가 제공하는 임시 디렉토리 (테스트 종료 시 자동 삭제됨)
    @TempDir
    Path tempUploadDir;

    private ImagePurpose testPurpose;

    @BeforeEach
    void setUp() throws IOException {
        // @Value 주입 대신 임시 디렉토리 경로를 생성자로 넘겨 객체 생성
        imageService = new ImageService(tempUploadDir.toString());

        // ImagePurpose Enum 중 첫 번째 값을 테스트용으로 사용
        testPurpose = ImagePurpose.values()[0];

        // 디렉토리 초기화 로직 실행
        imageService.init();
    }

    @Test
    @DisplayName("정상적인 이미지 파일 업로드 성공")
    void upload_Success() {
        // given
        MockMultipartFile file = new MockMultipartFile(
                "image",
                "test-image.png",
                "image/png",
                "dummy image content".getBytes()
        );

        // when
        ImageUploadResult result = imageService.upload(testPurpose, file);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getOriginalFilename()).isEqualTo("test-image.png");
        assertThat(result.getImageUrl()).contains(testPurpose.getUrl());
    }

    @Test
    @DisplayName("파일이 비어있으면 예외 발생")
    void upload_Fail_EmptyFile() {
        // given
        MockMultipartFile emptyFile = new MockMultipartFile("image", "empty.png", "image/png", new byte[0]);

        // when & then
        assertThatThrownBy(() -> imageService.upload(testPurpose, emptyFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("비어 있습니다");
    }

    @Test
    @DisplayName("허용되지 않은 Content-Type 업로드 시 예외 발생")
    void upload_Fail_InvalidContentType() {
        // given
        MockMultipartFile textFile = new MockMultipartFile(
                "image",
                "test.txt",
                "text/plain",
                "text content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> imageService.upload(testPurpose, textFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("형식의 이미지만 업로드");
    }

    @Test
    @DisplayName("확장자가 없는 파일 업로드 시 예외 발생")
    void upload_Fail_NoExtension() {
        // given
        MockMultipartFile noExtFile = new MockMultipartFile(
                "image",
                "filenameWithoutDot",
                "image/png",
                "content".getBytes()
        );

        // when & then
        assertThatThrownBy(() -> imageService.upload(testPurpose, noExtFile))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("확장자가 없는 파일");
    }

    @Test
    @DisplayName("존재하는 이미지 리소스 불러오기 성공")
    void loadAsResource_Success() throws IOException {
        // given: 먼저 파일을 업로드하여 저장
        MockMultipartFile file = new MockMultipartFile("image", "load-test.jpg", "image/jpeg", "content".getBytes());
        ImageUploadResult uploadResult = imageService.upload(testPurpose, file);

        String imageUrl = uploadResult.getImageUrl();
        String storedFileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);

        // when
        Resource resource = imageService.loadAsResource(testPurpose, storedFileName);

        // then
        assertThat(resource).isNotNull();
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 이미지 불러오기 시 DataNotFoundException 발생")
    void loadAsResource_Fail_NotFound() {
        // given
        String notFoundFileName = "not-exist-file.png";

        // when & then
        assertThatThrownBy(() -> imageService.loadAsResource(testPurpose, notFoundFileName))
                .isInstanceOf(DataNotFoundException.class);
    }
}