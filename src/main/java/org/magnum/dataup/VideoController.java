/*
 *
 * Copyright 2014 Jules White
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package org.magnum.dataup;

import org.magnum.dataup.model.Video;
import org.magnum.dataup.model.VideoStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Controller
public class VideoController {

    ArrayList<Video> videos = new ArrayList<>();

    @RequestMapping(value = "/video", method = RequestMethod.GET)
    public @ResponseBody
    List<Video> getVideos() {
        return videos;
    }

    @RequestMapping(value = "/video", method = RequestMethod.POST)
    public @ResponseBody
    Video addVideo(
            @RequestBody Video videoToUpload
    ) {
        videoToUpload.setId(System.currentTimeMillis());
        videoToUpload.setDataUrl(getDataUrl(videoToUpload.getId()));
        videos.add(videoToUpload);
        return videoToUpload;
    }

    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<VideoStatus> setVideoData(
            @PathVariable("id") long videoId,
            @RequestParam("data") MultipartFile dataInput
    ) throws IOException {

        Optional<Video> videoOptional = videos.stream()
                .filter(storedVideo -> storedVideo.getId() == videoId).findFirst();
        Video video;
        if (videoOptional.isPresent()) {
            video = videoOptional.get();
        } else {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Video not found", null);
        }

        try (InputStream stream = dataInput.getInputStream()) {
            VideoFileManager videoFileManager = VideoFileManager.get();
            videoFileManager.saveVideoData(video, stream);
        }

        return ResponseEntity.ok(new VideoStatus(VideoStatus.VideoState.READY));
    }

    @RequestMapping(value = "/video/{id}/data", method = RequestMethod.GET)
    public @ResponseBody
    ResponseEntity<byte[]> getVideoData(
            @PathVariable("id") long videoId
    ) throws IOException {

        Optional<Video> videoOptional = videos.stream()
                .filter(storedVideo -> storedVideo.getId() == videoId).findFirst();
        Video video;
        if (videoOptional.isPresent()) {
            video = videoOptional.get();
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        VideoFileManager videoFileManager = VideoFileManager.get();
        byte[] res = videoFileManager.getVideoData(video);

        return ResponseEntity.ok().body(res);
    }


    private String getDataUrl(long videoId) {
        return getUrlBaseForLocalServer() + "/video/" + videoId + "/data";
    }

    private String getUrlBaseForLocalServer() {
        HttpServletRequest request =
                ((ServletRequestAttributes) Objects.requireNonNull(RequestContextHolder.getRequestAttributes())).getRequest();
        return "http://" + request.getServerName()
                + ((request.getServerPort() != 80) ? ":" + request.getServerPort() : "");
    }
}
