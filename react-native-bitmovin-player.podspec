require 'json'

package = JSON.parse(File.read(File.join(__dir__, 'package.json')))

zipfile = "#{__dir__}/../react-native-bitmovin-player.zip"

Pod::Spec.new do |s|
  s.name         = package['name']
  s.version      = package['version']
  s.summary      = package['description']
  s.license      = package['license']
  s.authors      = package['author']
  s.homepage     = package['homepage']

  s.source            = { :git => 'https://github.com/netyweb/react-native-bitmovin-player.git' }
  s.source_files  = "ios/**/*.{h,m}"

  s.platform     = :ios, "12.0"

  s.dependency 'React-Core'
  s.dependency "BitmovinPlayer"

end
